package com.kusitms.kkium.experience.service.analyze;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kusitms.kkium.experience.dto.response.ExperienceAnalyzeResponse;
import com.kusitms.kkium.experience.service.llm.LlmService;
import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.global.exception.errorcode.ErrorCode;
import com.kusitms.kkium.notion.domain.NotionConnection;
import com.kusitms.kkium.notion.dto.response.NotionPageListResponse;
import com.kusitms.kkium.notion.repository.NotionConnectionRepository;
import com.kusitms.kkium.notion.utils.NotionApiClient;

@ExtendWith(MockitoExtension.class)
class NotionAnalyzeServiceTest {

  @Mock private NotionApiClient notionApiClient;
  @Mock private NotionConnectionRepository notionConnectionRepository;
  @Mock private LlmService llmService;

  private NotionAnalyzeService notionAnalyzeService;

  private static final Long USER_ID = 1L;
  private static final String PAGE_ID = "page-id-123";
  private static final String ACCESS_TOKEN = "test-access-token";
  private static final String PAGE_CONTENT = "노션 페이지 내용";

  @BeforeEach
  void setUp() {
    notionAnalyzeService =
        new NotionAnalyzeService(notionApiClient, notionConnectionRepository, llmService);
  }

  @Test
  @DisplayName("analyze: Notion 페이지 콘텐츠를 추출하고 LLM 분석 결과를 반환한다")
  void analyze_정상_흐름() {
    NotionConnection connection = mock(NotionConnection.class);
    ExperienceAnalyzeResponse expectedResponse = mock(ExperienceAnalyzeResponse.class);

    when(notionConnectionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(connection));
    when(connection.getAccessToken()).thenReturn(ACCESS_TOKEN);
    when(notionApiClient.getPageContent(ACCESS_TOKEN, PAGE_ID)).thenReturn(PAGE_CONTENT);
    when(llmService.analyze(USER_ID, PAGE_CONTENT)).thenReturn(expectedResponse);

    ExperienceAnalyzeResponse result = notionAnalyzeService.analyze(USER_ID, PAGE_ID);

    assertThat(result).isSameAs(expectedResponse);
    verify(notionApiClient).getPageContent(ACCESS_TOKEN, PAGE_ID);
    verify(llmService).analyze(USER_ID, PAGE_CONTENT);
  }

  @Test
  @DisplayName("analyze: Notion 연결이 없으면 NOTION_NOT_CONNECTED 예외를 던진다")
  void analyze_연결_없음() {
    when(notionConnectionRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> notionAnalyzeService.analyze(USER_ID, PAGE_ID))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.NOTION_NOT_CONNECTED);
  }

  @Test
  @DisplayName("getPages: Notion 페이지 목록을 정상 반환한다")
  void getPages_정상_흐름() {
    NotionConnection connection = mock(NotionConnection.class);
    List<NotionPageListResponse.NotionPageInfo> pageInfos = List.of();

    when(notionConnectionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(connection));
    when(connection.getAccessToken()).thenReturn(ACCESS_TOKEN);
    when(connection.getWorkspaceName()).thenReturn("테스트 워크스페이스");
    when(notionApiClient.getPages(ACCESS_TOKEN)).thenReturn(pageInfos);

    NotionPageListResponse result = notionAnalyzeService.getPages(USER_ID);

    assertThat(result.workspaceName()).isEqualTo("테스트 워크스페이스");
    assertThat(result.pages()).isEmpty();
  }

  @Test
  @DisplayName("getPages: Notion 연결이 없으면 NOTION_NOT_CONNECTED 예외를 던진다")
  void getPages_연결_없음() {
    when(notionConnectionRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> notionAnalyzeService.getPages(USER_ID))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.NOTION_NOT_CONNECTED);
  }
}
