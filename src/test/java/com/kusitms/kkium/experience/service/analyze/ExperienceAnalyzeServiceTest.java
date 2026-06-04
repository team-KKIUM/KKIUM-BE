package com.kusitms.kkium.experience.service.analyze;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.kusitms.kkium.experience.dto.response.ExperienceAnalyzeResponse;
import com.kusitms.kkium.experience.service.llm.LlmService;
import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.global.exception.errorcode.ErrorCode;
import com.kusitms.kkium.notion.domain.NotionConnection;
import com.kusitms.kkium.notion.repository.NotionConnectionRepository;
import com.kusitms.kkium.notion.utils.NotionApiClient;

@ExtendWith(MockitoExtension.class)
class ExperienceAnalyzeServiceTest {

  @Mock private PdfAnalyzeService pdfAnalyzeService;
  @Mock private NotionApiClient notionApiClient;
  @Mock private NotionConnectionRepository notionConnectionRepository;
  @Mock private LlmService llmService;

  private ExperienceAnalyzeService experienceAnalyzeService;

  private static final Long USER_ID = 1L;
  private static final String PAGE_ID = "page-id-123";
  private static final String ACCESS_TOKEN = "test-access-token";
  private static final String PDF_TEXT = "PDF 추출 텍스트";
  private static final String NOTION_TEXT = "Notion 페이지 내용";

  @BeforeEach
  void setUp() {
    experienceAnalyzeService =
        new ExperienceAnalyzeService(
            pdfAnalyzeService, notionApiClient, notionConnectionRepository, llmService);
  }

  private MockMultipartFile mockPdfFile() {
    return new MockMultipartFile("file", "test.pdf", "application/pdf", "pdf-bytes".getBytes());
  }

  @Test
  @DisplayName("PDF만 있을 때 PDF 텍스트를 추출하여 LLM 분석 결과를 반환한다")
  void analyze_PDF만() {
    MockMultipartFile file = mockPdfFile();
    ExperienceAnalyzeResponse expected = mock(ExperienceAnalyzeResponse.class);
    String combined = "[PDF 자료]\n" + PDF_TEXT + "\n\n";

    when(pdfAnalyzeService.extractText(file)).thenReturn(PDF_TEXT);
    when(llmService.analyze(USER_ID, combined)).thenReturn(expected);

    ExperienceAnalyzeResponse result = experienceAnalyzeService.analyze(USER_ID, file, null);

    assertThat(result).isSameAs(expected);
    verify(pdfAnalyzeService).extractText(file);
    verify(notionApiClient, never()).getPageContent(any(), any());
  }

  @Test
  @DisplayName("Notion만 있을 때 페이지 콘텐츠를 추출하여 LLM 분석 결과를 반환한다")
  void analyze_Notion만() {
    NotionConnection connection = mock(NotionConnection.class);
    ExperienceAnalyzeResponse expected = mock(ExperienceAnalyzeResponse.class);
    String combined = "[Notion 페이지]\n" + NOTION_TEXT;

    when(notionConnectionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(connection));
    when(connection.getAccessToken()).thenReturn(ACCESS_TOKEN);
    when(notionApiClient.getPageContent(ACCESS_TOKEN, PAGE_ID)).thenReturn(NOTION_TEXT);
    when(llmService.analyze(USER_ID, combined)).thenReturn(expected);

    ExperienceAnalyzeResponse result = experienceAnalyzeService.analyze(USER_ID, null, PAGE_ID);

    assertThat(result).isSameAs(expected);
    verify(notionApiClient).getPageContent(ACCESS_TOKEN, PAGE_ID);
    verify(pdfAnalyzeService, never()).extractText(any());
  }

  @Test
  @DisplayName("PDF와 Notion 둘 다 있을 때 텍스트를 합쳐서 LLM 분석 결과를 반환한다")
  void analyze_둘다_있음() {
    MockMultipartFile file = mockPdfFile();
    NotionConnection connection = mock(NotionConnection.class);
    ExperienceAnalyzeResponse expected = mock(ExperienceAnalyzeResponse.class);
    String combined = "[PDF 자료]\n" + PDF_TEXT + "\n\n[Notion 페이지]\n" + NOTION_TEXT;

    when(pdfAnalyzeService.extractText(file)).thenReturn(PDF_TEXT);
    when(notionConnectionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(connection));
    when(connection.getAccessToken()).thenReturn(ACCESS_TOKEN);
    when(notionApiClient.getPageContent(ACCESS_TOKEN, PAGE_ID)).thenReturn(NOTION_TEXT);
    when(llmService.analyze(USER_ID, combined)).thenReturn(expected);

    ExperienceAnalyzeResponse result = experienceAnalyzeService.analyze(USER_ID, file, PAGE_ID);

    assertThat(result).isSameAs(expected);
    verify(pdfAnalyzeService).extractText(file);
    verify(notionApiClient).getPageContent(ACCESS_TOKEN, PAGE_ID);
  }

  @Test
  @DisplayName("PDF와 Notion 둘 다 없으면 INVALID_INPUT_VALUE 예외를 던진다")
  void analyze_둘다_없음() {
    assertThatThrownBy(() -> experienceAnalyzeService.analyze(USER_ID, null, null))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
  }

  @Test
  @DisplayName("Notion 요청 시 연결이 없으면 NOTION_NOT_CONNECTED 예외를 던진다")
  void analyze_Notion_연결_없음() {
    when(notionConnectionRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> experienceAnalyzeService.analyze(USER_ID, null, PAGE_ID))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.NOTION_NOT_CONNECTED);
  }
}
