package com.kusitms.kkium.notion.utils;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.NOTION_TOKEN_FAILED;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@Component
public class NotionApiClient {

  private final WebClient webClient;
  private final ObjectMapper objectMapper = new ObjectMapper();

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
                          log.error(
                              "Notion 토큰 발급 실패 - status: {}, body: {}",
                              response.statusCode(),
                              errorBody);
                          return Mono.error(new BaseException(NOTION_TOKEN_FAILED));
                        });
              }
            })
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
    String responseBody =
        webClient
            .post()
            .uri("https://api.notion.com/v1/search")
            .header("Authorization", "Bearer " + accessToken)
            .header("Notion-Version", "2022-06-28")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("filter", Map.of("value", "page", "property", "object")))
            .exchangeToMono(
                res -> {
                  if (res.statusCode().is2xxSuccessful()) {
                    return res.bodyToMono(String.class);
                  } else {
                    return res.bodyToMono(String.class)
                        .flatMap(
                            err -> {
                              log.error("Notion 페이지 목록 조회 실패: {}", err);
                              return Mono.error(new BaseException(ErrorCode.NOTION_TOKEN_FAILED));
                            });
                  }
                })
            .block();

    List<NotionPageListResponse.NotionPageInfo> leafPages = new ArrayList<>();
    Set<String> visitedPageIds = new HashSet<>();
    try {
      JsonNode response = objectMapper.readTree(responseBody);
      if (response != null && response.has("results")) {
        for (JsonNode page : response.get("results")) {
          String pageId = page.get("id").asText();
          String title = extractPageTitle(page);
          collectLeafPages(accessToken, pageId, title, leafPages, visitedPageIds, 0);
        }
      }
    } catch (JsonProcessingException e) {
      log.error("Notion 페이지 목록 파싱 실패: {}", e.getMessage(), e);
      throw new BaseException(ErrorCode.NOTION_RESPONSE_PARSE_ERROR);
    }
    return leafPages;
  }

  // 하위 페이지 재귀 탐색 — child_page 없는 leaf만 수집
  private void collectLeafPages(
      String accessToken,
      String pageId,
      String title,
      List<NotionPageListResponse.NotionPageInfo> leafPages,
      Set<String> visitedPageIds,
      int depth) {

    if (depth > MAX_BLOCK_FETCH_DEPTH) return;
    if (visitedPageIds.contains(pageId)) return;
    visitedPageIds.add(pageId);

    String responseBody =
        webClient
            .get()
            .uri("https://api.notion.com/v1/blocks/" + pageId + "/children")
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
                              log.error("Notion 하위 블록 조회 실패: {}", err);
                              return Mono.error(new BaseException(ErrorCode.NOTION_TOKEN_FAILED));
                            });
                  }
                })
            .block();

    try {
      JsonNode response = objectMapper.readTree(responseBody);
      if (response == null || !response.has("results")) {
        leafPages.add(new NotionPageListResponse.NotionPageInfo(pageId, title));
        return;
      }

      List<JsonNode> childPages = new ArrayList<>();
      for (JsonNode block : response.get("results")) {
        if ("child_page".equals(block.get("type").asText())) {
          childPages.add(block);
        }
      }

      if (childPages.isEmpty()) {
        // 하위 페이지 없음 → leaf
        leafPages.add(new NotionPageListResponse.NotionPageInfo(pageId, title));
      } else {
        // 하위 페이지 있음 → 재귀
        for (JsonNode child : childPages) {
          String childId = child.get("id").asText();
          String childTitle = child.get("child_page").get("title").asText("제목 없음");
          collectLeafPages(accessToken, childId, childTitle, leafPages, visitedPageIds, depth + 1);
        }
      }
    } catch (JsonProcessingException e) {
      log.error("Notion 하위 페이지 파싱 실패: {}", e.getMessage(), e);
      throw new BaseException(ErrorCode.NOTION_BLOCK_PARSE_ERROR);
    }
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
                              log.error("Notion 블록 조회 실패: {}", err);
                              return Mono.error(new BaseException(ErrorCode.NOTION_TOKEN_FAILED));
                            });
                  }
                })
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

  private String extractPageTitle(JsonNode page) {
    try {
      JsonNode properties = page.get("properties");
      if (properties == null) return "제목 없음";
      // title 또는 Name 필드에서 추출
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
