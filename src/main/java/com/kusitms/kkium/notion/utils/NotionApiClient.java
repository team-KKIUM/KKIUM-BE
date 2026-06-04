package com.kusitms.kkium.notion.utils;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.NOTION_TOKEN_FAILED;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.global.exception.errorcode.ErrorCode;
import com.kusitms.kkium.notion.dto.response.NotionPageListResponse;
import com.kusitms.kkium.notion.dto.response.NotionTokenResponse;

import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Slf4j
@RequiredArgsConstructor
@Component
public class NotionApiClient {

  private final WebClient webClient;
  private final ObjectMapper objectMapper = new ObjectMapper();

  // Notion API rate limiter (공식 평균 초당 3 req 제한 준수)
  private final Bucket notionRateLimiter =
      Bucket.builder()
          .addLimit(limit -> limit.capacity(3).refillGreedy(3, Duration.ofSeconds(1)))
          .build();

  private static final String TOKEN_URI = "https://api.notion.com/v1/oauth/token";
  private static final int MAX_BLOCK_FETCH_DEPTH = 5;

  @Value("${notion.client-id}")
  private String clientId;

  @Value("${notion.client-secret}")
  private String clientSecret;

  @Value("${notion.redirect-uri}")
  private String redirectUri;

  // 인가 코드 → access_token 교환
  public NotionTokenResponse getAccessToken(String code) {
    String credentials =
        Base64.getEncoder()
            .encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

    return webClient
        .post()
        .uri(TOKEN_URI)
        .header("Authorization", "Basic " + credentials)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            Map.of(
                "grant_type", "authorization_code",
                "code", code,
                "redirect_uri", redirectUri))
        .exchangeToMono(
            response -> {
              if (response.statusCode().is2xxSuccessful()) {
                return response.bodyToMono(NotionTokenResponse.class);
              } else {
                return response
                    .bodyToMono(String.class)
                    .flatMap(
                        errorBody -> {
                          int status = response.statusCode().value();
                          boolean retryable =
                              status == 429 || response.statusCode().is5xxServerError();
                          log.error("Notion 토큰 발급 실패 - status: {}, body: {}", status, errorBody);
                          if (retryable) {
                            return Mono.error(
                                new NotionRetryableException(
                                    "Notion 토큰 발급 일시적 실패 (status=" + status + ")"));
                          }
                          return Mono.error(new BaseException(NOTION_TOKEN_FAILED));
                        });
              }
            })
        .doFirst(this::acquireNotionRateLimit)
        .retryWhen(notionRetrySpec())
        .onErrorMap(NotionRetryableException.class, ex -> new BaseException(NOTION_TOKEN_FAILED))
        .block();
  }

  // OAuth 인증 URL 생성
  public String getAuthorizationUrl(String state) {
    return UriComponentsBuilder.fromUriString("https://api.notion.com/v1/oauth/authorize")
        .queryParam("client_id", clientId)
        .queryParam("response_type", "code")
        .queryParam("owner", "user")
        .queryParam("redirect_uri", redirectUri)
        .queryParam("state", state)
        .build()
        .toUriString();
  }

  // 접근 가능한 최하단(leaf) 페이지 목록 조회
  public List<NotionPageListResponse.NotionPageInfo> getPages(String accessToken) {
    // 1. /v1/search 페이징으로 모든 페이지 메타데이터 수집
    List<JsonNode> allPages = fetchAllPagesViaSearch(accessToken);

    // 2. 누군가의 부모로 등장하는 페이지 id 집합 구성
    Set<String> parentIds =
        allPages.stream()
            .map(this::extractParentId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    // 3. leaf 판별
    //    - database 타입: 자기 자신 + 안의 row 모두 추가 (기존 동작 유지)
    //    - DB row(parent.type == database_id)는 fetchDatabaseRows에서 처리되므로 일반 페이지 루프에서 스킵
    //    - 일반 페이지: parentIds에 없는 경우에만 leaf로 추가
    List<NotionPageListResponse.NotionPageInfo> leafs = new ArrayList<>();
    for (JsonNode page : allPages) {
      String type = page.has("object") ? page.get("object").asText() : "page";
      String pageId = page.get("id").asText();

      if ("database".equals(type)) {
        leafs.add(toPageInfo(page));
        fetchDatabaseRows(accessToken, pageId, leafs);
      } else if (!isDatabaseRow(page) && !parentIds.contains(pageId)) {
        leafs.add(toPageInfo(page));
      }
    }

    return leafs;
  }

  // parent.type이 database_id인지 확인 (DB row 여부)
  private boolean isDatabaseRow(JsonNode page) {
    JsonNode parent = page.get("parent");
    if (parent == null || parent.isNull()) return false;
    String parentType = parent.has("type") ? parent.get("type").asText() : null;
    return "database_id".equals(parentType);
  }

  // Notion API 호출 직전 rate limit 토큰 소비 (초당 3 req 초과 시 대기)
  private void acquireNotionRateLimit() {
    try {
      notionRateLimiter.asBlocking().consume(1);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Notion rate limit wait interrupted", e);
    }
  }

  // 일시적 실패(5xx, 429, 네트워크 오류)에 대한 재시도 정책: 최대 3회, 지수 백오프 (1초 → 2초 → 4초)
  private Retry notionRetrySpec() {
    return Retry.backoff(3, Duration.ofSeconds(1)).filter(NotionApiClient::isRetryable);
  }

  // 재시도 대상 예외 판별 (5xx/429 + 네트워크 일시 오류)
  private static boolean isRetryable(Throwable t) {
    if (t instanceof NotionRetryableException) return true;
    if (t instanceof IOException) return true;
    return t.getCause() instanceof IOException;
  }

  // Notion API 일시적 실패(5xx, 429) 시 재시도 대상으로 표시
  private static class NotionRetryableException extends RuntimeException {
    NotionRetryableException(String message) {
      super(message);
    }
  }

  // /v1/search 페이징 호출하여 모든 페이지 메타데이터 수집
  List<JsonNode> fetchAllPagesViaSearch(String accessToken) {
    List<JsonNode> allPages = new ArrayList<>();
    String nextCursor = null;

    do {
      Map<String, Object> body = new HashMap<>();
      if (nextCursor != null) {
        body.put("start_cursor", nextCursor);
      }
      final String currentCursor = nextCursor;

      String responseBody =
          webClient
              .post()
              .uri("https://api.notion.com/v1/search")
              .header("Authorization", "Bearer " + accessToken)
              .header("Notion-Version", "2022-06-28")
              .contentType(MediaType.APPLICATION_JSON)
              .bodyValue(body)
              .exchangeToMono(
                  res -> {
                    if (res.statusCode().is2xxSuccessful()) {
                      return res.bodyToMono(String.class);
                    } else {
                      return res.bodyToMono(String.class)
                          .flatMap(
                              err -> {
                                int status = res.statusCode().value();
                                boolean retryable =
                                    status == 429 || res.statusCode().is5xxServerError();
                                log.error(
                                    "Notion 페이지 목록 조회 실패 (cursor={}, status={}): {}",
                                    currentCursor,
                                    status,
                                    err);
                                if (retryable) {
                                  return Mono.error(
                                      new NotionRetryableException(
                                          "Notion search 일시적 실패 (status=" + status + ")"));
                                }
                                return Mono.error(new BaseException(ErrorCode.NOTION_TOKEN_FAILED));
                              });
                    }
                  })
              .doFirst(this::acquireNotionRateLimit)
              .retryWhen(notionRetrySpec())
              .onErrorMap(
                  NotionRetryableException.class,
                  ex -> new BaseException(ErrorCode.NOTION_TOKEN_FAILED))
              .block();

      try {
        if (responseBody == null || responseBody.isBlank()) break;
        JsonNode response = objectMapper.readTree(responseBody);
        if (response == null) break;

        if (response.has("results")) {
          for (JsonNode page : response.get("results")) {
            allPages.add(page);
          }
        }

        nextCursor =
            response.hasNonNull("has_more") && response.get("has_more").asBoolean()
                ? response.path("next_cursor").asText(null)
                : null;
      } catch (JsonProcessingException e) {
        log.error("Notion 페이지 목록 파싱 실패: {}", e.getMessage(), e);
        throw new BaseException(ErrorCode.NOTION_RESPONSE_PARSE_ERROR);
      }
    } while (nextCursor != null);

    return allPages;
  }

  // database row 조회하여 leaf 목록에 추가
  void fetchDatabaseRows(
      String accessToken, String databaseId, List<NotionPageListResponse.NotionPageInfo> leafs) {
    try {
      String responseBody =
          webClient
              .post()
              .uri("https://api.notion.com/v1/databases/" + databaseId + "/query")
              .header("Authorization", "Bearer " + accessToken)
              .header("Notion-Version", "2022-06-28")
              .contentType(MediaType.APPLICATION_JSON)
              .bodyValue(Map.of())
              .exchangeToMono(
                  res -> {
                    if (res.statusCode().is2xxSuccessful()) {
                      return res.bodyToMono(String.class);
                    } else {
                      return res.bodyToMono(String.class)
                          .flatMap(
                              err -> {
                                int status = res.statusCode().value();
                                boolean retryable =
                                    status == 429 || res.statusCode().is5xxServerError();
                                log.warn(
                                    "Notion DB 쿼리 실패 (databaseId={}, status={}): {}",
                                    databaseId,
                                    status,
                                    err);
                                if (retryable) {
                                  return Mono.error(
                                      new NotionRetryableException(
                                          "Notion DB 쿼리 일시적 실패 (status=" + status + ")"));
                                }
                                return Mono.just("");
                              });
                    }
                  })
              .doFirst(this::acquireNotionRateLimit)
              .retryWhen(notionRetrySpec())
              .onErrorResume(
                  NotionRetryableException.class,
                  ex -> {
                    log.warn(
                        "Notion DB 쿼리 재시도 실패 (databaseId={}): {}", databaseId, ex.getMessage());
                    return Mono.just("");
                  })
              .block();

      if (responseBody == null || responseBody.isBlank()) return;
      JsonNode response = objectMapper.readTree(responseBody);
      if (response == null || !response.has("results")) return;

      for (JsonNode row : response.get("results")) {
        leafs.add(toPageInfo(row));
      }
    } catch (Exception e) {
      log.warn("Notion DB row 파싱 실패 (databaseId={}): {}", databaseId, e.getMessage());
    }
  }

  // JsonNode → NotionPageInfo 변환
  private NotionPageListResponse.NotionPageInfo toPageInfo(JsonNode page) {
    String pageId = page.get("id").asText();
    String title = extractPageTitle(page);
    String icon = extractIcon(page);
    String type = page.has("object") ? page.get("object").asText() : "page";
    String lastEditedTime =
        page.hasNonNull("last_edited_time") ? page.get("last_edited_time").asText() : null;
    String parentId = extractParentId(page);
    return new NotionPageListResponse.NotionPageInfo(
        pageId, title, icon, type, lastEditedTime, parentId);
  }

  // 페이지 블록 콘텐츠 텍스트 추출
  public String getPageContent(String accessToken, String pageId) {
    return fetchBlockChildren(accessToken, pageId, 0);
  }

  private String fetchBlockChildren(String accessToken, String blockId, int depth) {
    if (depth > MAX_BLOCK_FETCH_DEPTH) return "";

    String responseBody =
        webClient
            .get()
            .uri("https://api.notion.com/v1/blocks/" + blockId + "/children")
            .header("Authorization", "Bearer " + accessToken)
            .header("Notion-Version", "2022-06-28")
            .exchangeToMono(
                res -> {
                  if (res.statusCode().is2xxSuccessful()) {
                    return res.bodyToMono(String.class);
                  } else {
                    return res.bodyToMono(String.class)
                        .flatMap(
                            err -> {
                              int status = res.statusCode().value();
                              boolean retryable =
                                  status == 429 || res.statusCode().is5xxServerError();
                              log.error("Notion 블록 조회 실패 (status={}): {}", status, err);
                              if (retryable) {
                                return Mono.error(
                                    new NotionRetryableException(
                                        "Notion 블록 조회 일시적 실패 (status=" + status + ")"));
                              }
                              return Mono.error(new BaseException(ErrorCode.NOTION_TOKEN_FAILED));
                            });
                  }
                })
            .doFirst(this::acquireNotionRateLimit)
            .retryWhen(notionRetrySpec())
            .onErrorMap(
                NotionRetryableException.class,
                ex -> new BaseException(ErrorCode.NOTION_TOKEN_FAILED))
            .block();

    StringBuilder sb = new StringBuilder();
    try {
      JsonNode response = objectMapper.readTree(responseBody);
      if (response != null && response.has("results")) {
        for (JsonNode block : response.get("results")) {
          String blockType = block.get("type").asText();
          String text = extractTextFromBlock(block, blockType);
          if (!text.isBlank()) {
            sb.append(text).append("\n");
          }
          if (block.has("has_children") && block.get("has_children").asBoolean()) {
            sb.append(fetchBlockChildren(accessToken, block.get("id").asText(), depth + 1));
          }
        }
      }
    } catch (JsonProcessingException e) {
      log.error("Notion 블록 파싱 실패: {}", e.getMessage(), e);
      throw new BaseException(ErrorCode.NOTION_BLOCK_PARSE_ERROR);
    }
    return sb.toString();
  }

  private String extractTextFromBlock(JsonNode block, String blockType) {
    JsonNode typeNode = block.get(blockType);
    if (typeNode == null || !typeNode.has("rich_text")) return "";

    StringBuilder sb = new StringBuilder();
    for (JsonNode rt : typeNode.get("rich_text")) {
      if (rt.has("plain_text")) {
        sb.append(rt.get("plain_text").asText());
      }
    }
    return sb.toString();
  }

  private String extractParentId(JsonNode page) {
    try {
      JsonNode parent = page.get("parent");
      if (parent == null || parent.isNull()) return null;
      String parentType = parent.has("type") ? parent.get("type").asText() : null;
      if ("page_id".equals(parentType)) {
        return parent.get("page_id").asText();
      } else if ("database_id".equals(parentType)) {
        return parent.get("database_id").asText();
      }
    } catch (Exception e) {
      log.warn("parentId 추출 실패: {}", e.getMessage());
    }
    return null;
  }

  private String extractIcon(JsonNode page) {
    try {
      JsonNode icon = page.get("icon");
      if (icon == null || icon.isNull()) return null;
      String iconType = icon.has("type") ? icon.get("type").asText() : null;
      if ("emoji".equals(iconType)) {
        return icon.get("emoji").asText();
      } else if ("external".equals(iconType)) {
        return icon.get("external").get("url").asText();
      } else if ("file".equals(iconType)) {
        return icon.get("file").get("url").asText();
      }
    } catch (Exception e) {
      log.warn("페이지 아이콘 추출 실패: {}", e.getMessage(), e);
    }
    return null;
  }

  private String extractPageTitle(JsonNode page) {
    try {
      // database는 title 필드가 최상위에 배열로 존재
      String objectType = page.has("object") ? page.get("object").asText() : "";
      if ("database".equals(objectType)) {
        JsonNode titleArray = page.get("title");
        if (titleArray != null && titleArray.isArray() && titleArray.size() > 0) {
          return titleArray.get(0).get("plain_text").asText("제목 없음");
        }
        return "제목 없음";
      }

      JsonNode properties = page.get("properties");
      if (properties == null) return "제목 없음";
      for (JsonNode prop : properties) {
        if (prop.has("type") && prop.get("type").asText().equals("title")) {
          JsonNode titleArray = prop.get("title");
          if (titleArray != null && titleArray.isArray() && titleArray.size() > 0) {
            return titleArray.get(0).get("plain_text").asText();
          }
        }
      }
    } catch (Exception e) {
      log.warn("페이지 제목 추출 실패: {}", e.getMessage(), e);
    }
    return "제목 없음";
  }
}
