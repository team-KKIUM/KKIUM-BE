package com.kusitms.kkium.experience.service.analyze;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
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

@ExtendWith(MockitoExtension.class)
class PdfAnalyzeServiceTest {

  @Mock private LlmService llmService;

  private PdfAnalyzeService pdfAnalyzeService;
  private byte[] validPdfBytes;

  @BeforeEach
  void setUp() throws Exception {
    pdfAnalyzeService = new PdfAnalyzeService(llmService);

    // 최소 유효 PDF bytes 생성 (빈 페이지 1개)
    try (PDDocument doc = new PDDocument()) {
      doc.addPage(new PDPage());
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      doc.save(baos);
      validPdfBytes = baos.toByteArray();
    }
  }

  @Test
  @DisplayName("유효한 PDF 파일이면 텍스트를 추출하여 LLM 분석 결과를 반환한다")
  void analyzePdf_정상_흐름() {
    ExperienceAnalyzeResponse expectedResponse = mock(ExperienceAnalyzeResponse.class);
    MockMultipartFile file =
        new MockMultipartFile("file", "test.pdf", "application/pdf", validPdfBytes);
    when(llmService.analyze(eq(1L), any(String.class))).thenReturn(expectedResponse);

    ExperienceAnalyzeResponse result = pdfAnalyzeService.analyzePdf(1L, file);

    assertThat(result).isSameAs(expectedResponse);
    verify(llmService).analyze(eq(1L), any(String.class));
  }

  @Test
  @DisplayName("PDF가 아닌 파일 타입이면 INVALID_FILE_TYPE 예외를 던진다")
  void analyzePdf_잘못된_파일타입() {
    MockMultipartFile file =
        new MockMultipartFile("file", "test.txt", "text/plain", "텍스트".getBytes());

    assertThatThrownBy(() -> pdfAnalyzeService.analyzePdf(1L, file))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_FILE_TYPE);
  }

  @Test
  @DisplayName("contentType이 null이면 INVALID_FILE_TYPE 예외를 던진다")
  void analyzePdf_contentType_null() {
    MockMultipartFile file = new MockMultipartFile("file", "test.pdf", null, validPdfBytes);

    assertThatThrownBy(() -> pdfAnalyzeService.analyzePdf(1L, file))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_FILE_TYPE);
  }

  @Test
  @DisplayName("PDF 파싱에 실패하면 PDF_PARSE_FAILED 예외를 던진다")
  void extractText_파싱_실패() {
    MockMultipartFile file =
        new MockMultipartFile("file", "test.pdf", "application/pdf", "not a pdf".getBytes());

    assertThatThrownBy(() -> pdfAnalyzeService.extractText(file))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PDF_PARSE_FAILED);
  }
}
