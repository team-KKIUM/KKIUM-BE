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
            .bodyValue(Map.of())
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
          String icon = extractIcon(page);
          String type = page.has("object") ? page.get("object").asText() : "page";
          String lastEditedTime =
              page.hasNonNull("last_edited_time") ? page.get("last_edited_time").asText() : null;
          String parentId = extractParentId(page);
          collectLeafPages(
              accessToken,
              pageId,
              title,
              icon,
              type,
              lastEditedTime,
              parentId,
              leafPages,
              visitedPageIds,
              0);
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
      String icon,
      String type,
      String lastEditedTime,
      String parentId,
      List<NotionPageListResponse.NotionPageInfo> leafPages,
      Set<String> visitedPageIds,
      int depth) {

    if (depth > MAX_BLOCK_FETCH_DEPTH) return;
    if (visitedPageIds.contains(pageId)) return;
    visitedPageIds.add(pageId);

    // database는 자체도 leaf로 추가하고, 하위 row 페이지들도 추가
    if ("database".equals(type)) {
      leafPages.add(
          new NotionPageListResponse.NotionPageInfo(
              pageId, title, icon, type, lastEditedTime, parentId));
      fetchDatabaseRows(
          accessToken, pageId, icon, lastEditedTime, leafPages, visitedPageIds, depth);
      return;
    }

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
        leafPages.add(
            new NotionPageListResponse.NotionPageInfo(
                pageId, title, icon, type, lastEditedTime, parentId));
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
        leafPages.add(
            new NotionPageListResponse.NotionPageInfo(
                pageId, title, icon, type, lastEditedTime, parentId));
      } else {
        // 하위 페이지 있음 → fetchPage 병렬 호출 후 재귀
        List<String> childIds = childPages.stream().map(child -> child.path("id").asText()).toList();
        List<String> childTitles =
            childPages.stream()
                .map(child -> child.path("child_page").path("title").asText("제목 없음"))
                .toList();

        List<Mono<JsonNode>> fetchMonos =
            childIds.stream().map(childId -> fetchPageAsync(accessToken, childId)).toList();

        List<JsonNode> childPageNodes =
            Mono.zip(
                    fetchMonos,
                    results -> java.util.Arrays.stream(results).map(r -> (JsonNode) r).toList())
                .blockOptional()
                .orElse(List.of());

        for (int i = 0; i < childIds.size(); i++) {
          String childId = childIds.get(i);
          String childTitle = childTitles.get(i);
          JsonNode childPage = i < childPageNodes.size() ? childPageNodes.get(i) : null;
          String childIcon = childPage != null ? extractIcon(childPage) : null;
          String childLastEditedTime =
              childPage != null && childPage.has("last_edited_time")
                  ? childPage.get("last_edited_time").asText()
                  : lastEditedTime;
          collectLeafPages(
              accessToken,
              childId,
              childTitle,
              childIcon,
              type,
              childLastEditedTime,
              pageId,
              leafPages,
              visitedPageIds,
              depth + 1);
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

  private void fetchDatabaseRows(
      String accessToken,
      String databaseId,
      String parentIcon,
      String parentLastEditedTime,
      List<NotionPageListResponse.NotionPageInfo> leafPages,
      Set<String> visitedPageIds,
      int depth) {
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
                                log.warn("Notion DB 쿼리 실패 (databaseId={}): {}", databaseId, err);
                                return Mono.just("");
                              });
                    }
                  })
              .block();

      if (responseBody == null || responseBody.isBlank()) return;
      JsonNode response = objectMapper.readTree(responseBody);
      if (response == null || !response.has("results")) return;

      for (JsonNode row : response.get("results")) {
        String rowId = row.get("id").asText();
        String rowTitle = extractPageTitle(row);
        String rowIcon = extractIcon(row);
        String rowLastEditedTime =
            row.hasNonNull("last_edited_time")
                ? row.get("last_edited_time").asText()
                : parentLastEditedTime;
        collectLeafPages(
            accessToken,
            rowId,
            rowTitle,
            rowIcon,
            "page",
            rowLastEditedTime,
            databaseId,
            leafPages,
            visitedPageIds,
            depth + 1);
      }
    } catch (Exception e) {
      log.warn("Notion DB row 파싱 실패 (databaseId={}): {}", databaseId, e.getMessage());
    }
  }

  private Mono<JsonNode> fetchPageAsync(String accessToken, String pageId) {
    return webClient
        .get()
        .uri("https://api.notion.com/v1/pages/" + pageId)
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
                          log.warn("Notion 페이지 조회 실패 (pageId={}): {}", pageId, err);
                          return Mono.just("");
                        });
              }
            })
        .mapNotNull(
            body -> {
              if (body == null || body.isBlank()) return null;
              try {
                return objectMapper.readTree(body);
              } catch (Exception e) {
                log.warn("Notion 페이지 파싱 실패 (pageId={}): {}", pageId, e.getMessage());
                return null;
              }
            })
        .onErrorReturn(objectMapper.createObjectNode());
  }

  private JsonNode fetchPage(String accessToken, String pageId) {
    try {
      String responseBody =
          webClient
              .get()
              .uri("https://api.notion.com/v1/pages/" + pageId)
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
                                log.warn("Notion 페이지 조회 실패 (pageId={}): {}", pageId, err);
                                return Mono.just("");
                              });
                    }
                  })
              .block();
      if (responseBody == null || responseBody.isBlank()) return null;
      return objectMapper.readTree(responseBody);
    } catch (Exception e) {
      log.warn("Notion 페이지 조회 예외 (pageId={}): {}", pageId, e.getMessage());
      return null;
    }
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
