package com.kusitms.kkium.notion.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kusitms.kkium.notion.dto.response.NotionPageListResponse;

@ExtendWith(MockitoExtension.class)
class NotionApiClientTest {

  @Mock private WebClient webClient;

  @Spy @InjectMocks private NotionApiClient notionApiClient;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private static final String ACCESS_TOKEN = "test-access-token";

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(notionApiClient, "clientId", "test-client-id");
    ReflectionTestUtils.setField(notionApiClient, "clientSecret", "test-client-secret");
    ReflectionTestUtils.setField(notionApiClient, "redirectUri", "http://localhost:8080/callback");
  }

  // ── fixture builders ──────────────────────────────────────────────────────

  private JsonNode buildPage(String id, String parentType, String parentId, String title)
      throws Exception {
    String parentJson =
        switch (parentType) {
          case "workspace" -> "{\"type\":\"workspace\",\"workspace\":true}";
          case "page_id" -> "{\"type\":\"page_id\",\"page_id\":\"" + parentId + "\"}";
          case "database_id" -> "{\"type\":\"database_id\",\"database_id\":\"" + parentId + "\"}";
          default -> "{\"type\":\"" + parentType + "\"}";
        };
    String json =
        """
        {
          "id": "%s",
          "object": "page",
          "parent": %s,
          "properties": {
            "제목": {"type": "title", "title": [{"plain_text": "%s"}]}
          },
          "last_edited_time": "2024-01-01T00:00:00.000Z"
        }
        """
            .formatted(id, parentJson, title);
    return objectMapper.readTree(json);
  }

  private JsonNode buildDatabase(String id, String title) throws Exception {
    String json =
        """
        {
          "id": "%s",
          "object": "database",
          "title": [{"plain_text": "%s"}],
          "parent": {"type": "workspace", "workspace": true},
          "last_edited_time": "2024-01-01T00:00:00.000Z"
        }
        """
            .formatted(id, title);
    return objectMapper.readTree(json);
  }

  // ── tests ────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("부모 없는 페이지는 leaf로 반환된다")
  void getPages_루트_페이지_leaf() throws Exception {
    JsonNode page = buildPage("page-1", "workspace", null, "루트 페이지");
    doReturn(List.of(page)).when(notionApiClient).fetchAllPagesViaSearch(ACCESS_TOKEN);

    List<NotionPageListResponse.NotionPageInfo> result = notionApiClient.getPages(ACCESS_TOKEN);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).pageId()).isEqualTo("page-1");
  }

  @Test
  @DisplayName("자식 페이지가 있는 부모 페이지는 leaf에서 제외되고 자식만 leaf로 반환된다")
  void getPages_부모페이지_leaf_제외() throws Exception {
    JsonNode parent = buildPage("parent-id", "workspace", null, "부모");
    JsonNode child = buildPage("child-id", "page_id", "parent-id", "자식");
    doReturn(List.of(parent, child)).when(notionApiClient).fetchAllPagesViaSearch(ACCESS_TOKEN);

    List<NotionPageListResponse.NotionPageInfo> result = notionApiClient.getPages(ACCESS_TOKEN);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).pageId()).isEqualTo("child-id");
  }

  @Test
  @DisplayName("database 타입 페이지는 leaf에 포함되고 database row 조회가 호출된다")
  void getPages_database_leaf_포함() throws Exception {
    JsonNode db = buildDatabase("db-id", "데이터베이스");
    doReturn(List.of(db)).when(notionApiClient).fetchAllPagesViaSearch(ACCESS_TOKEN);
    doNothing().when(notionApiClient).fetchDatabaseRows(anyString(), anyString(), any());

    List<NotionPageListResponse.NotionPageInfo> result = notionApiClient.getPages(ACCESS_TOKEN);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).pageId()).isEqualTo("db-id");
    verify(notionApiClient).fetchDatabaseRows(eq(ACCESS_TOKEN), eq("db-id"), any());
  }

  @Test
  @DisplayName("parent.type이 database_id인 페이지(DB row)는 일반 루프에서 스킵된다")
  void getPages_database_row_스킵() throws Exception {
    JsonNode dbRow = buildPage("row-id", "database_id", "db-id", "DB 행");
    doReturn(List.of(dbRow)).when(notionApiClient).fetchAllPagesViaSearch(ACCESS_TOKEN);

    List<NotionPageListResponse.NotionPageInfo> result = notionApiClient.getPages(ACCESS_TOKEN);

    // database row는 일반 페이지 루프에서 스킵 (fetchDatabaseRows에서 처리)
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("getAuthorizationUrl은 state와 clientId가 포함된 URL을 반환한다")
  void getAuthorizationUrl_state_포함() {
    String state = "my-test-state";

    String url = notionApiClient.getAuthorizationUrl(state);

    assertThat(url).contains("client_id=test-client-id");
    assertThat(url).contains("state=" + state);
    assertThat(url).contains("redirect_uri=");
  }
}
