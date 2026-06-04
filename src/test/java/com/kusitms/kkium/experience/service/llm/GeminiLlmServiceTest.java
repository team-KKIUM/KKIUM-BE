package com.kusitms.kkium.experience.service.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kusitms.kkium.experience.dto.response.ExperienceAnalyzeResponse;
import com.kusitms.kkium.experience.service.llm.pipeline.ExperiencePromptBuilder;
import com.kusitms.kkium.experience.service.llm.pipeline.ExperienceResponseParser;
import com.kusitms.kkium.experience.service.llm.pipeline.GeminiApiClient;
import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.global.exception.errorcode.ErrorCode;

@ExtendWith(MockitoExtension.class)
class GeminiLlmServiceTest {

  @Mock private ExperiencePromptBuilder promptBuilder;
  @Mock private GeminiApiClient apiClient;
  @Mock private ExperienceResponseParser responseParser;

  private GeminiLlmService geminiLlmService;

  private static final Long USER_ID = 1L;
  private static final String EXTRACTED_TEXT = "추출된 텍스트";
  private static final String BUILT_PROMPT = "빌드된 프롬프트";
  private static final String RAW_JSON = "{\"raw\":\"json\"}";

  @BeforeEach
  void setUp() {
    geminiLlmService = new GeminiLlmService(promptBuilder, apiClient, responseParser);
  }

  @Test
  @DisplayName("정상 흐름: prompt 빌드 → API 호출 → 파싱 순서로 동작하고 결과를 반환한다")
  void analyze_정상_흐름() {
    ExperienceAnalyzeResponse expectedResponse = mock(ExperienceAnalyzeResponse.class);
    when(promptBuilder.build(EXTRACTED_TEXT)).thenReturn(BUILT_PROMPT);
    when(apiClient.call(BUILT_PROMPT)).thenReturn(RAW_JSON);
    when(responseParser.parse(RAW_JSON)).thenReturn(expectedResponse);

    ExperienceAnalyzeResponse result = geminiLlmService.analyze(USER_ID, EXTRACTED_TEXT);

    assertThat(result).isSameAs(expectedResponse);
    InOrder inOrder = Mockito.inOrder(promptBuilder, apiClient, responseParser);
    inOrder.verify(promptBuilder).build(EXTRACTED_TEXT);
    inOrder.verify(apiClient).call(BUILT_PROMPT);
    inOrder.verify(responseParser).parse(RAW_JSON);
  }

  @Test
  @DisplayName("LLM_RESPONSE_INVALID 발생 시 1회 재시도 후 정상 결과를 반환한다")
  void analyze_파싱실패_재시도_성공() {
    ExperienceAnalyzeResponse expectedResponse = mock(ExperienceAnalyzeResponse.class);
    when(promptBuilder.build(EXTRACTED_TEXT)).thenReturn(BUILT_PROMPT);
    when(apiClient.call(BUILT_PROMPT)).thenReturn(RAW_JSON);
    when(responseParser.parse(RAW_JSON))
        .thenThrow(new BaseException(ErrorCode.LLM_RESPONSE_INVALID))
        .thenReturn(expectedResponse);

    ExperienceAnalyzeResponse result = geminiLlmService.analyze(USER_ID, EXTRACTED_TEXT);

    assertThat(result).isSameAs(expectedResponse);
    verify(promptBuilder, times(1)).build(EXTRACTED_TEXT);
    verify(apiClient, times(2)).call(BUILT_PROMPT);
    verify(responseParser, times(2)).parse(RAW_JSON);
  }

  @Test
  @DisplayName("LLM_SCHEMA_VIOLATION 발생 시 1회 재시도 후 정상 결과를 반환한다")
  void analyze_스키마위반_재시도_성공() {
    ExperienceAnalyzeResponse expectedResponse = mock(ExperienceAnalyzeResponse.class);
    when(promptBuilder.build(EXTRACTED_TEXT)).thenReturn(BUILT_PROMPT);
    when(apiClient.call(BUILT_PROMPT)).thenReturn(RAW_JSON);
    when(responseParser.parse(RAW_JSON))
        .thenThrow(new BaseException(ErrorCode.LLM_SCHEMA_VIOLATION))
        .thenReturn(expectedResponse);

    ExperienceAnalyzeResponse result = geminiLlmService.analyze(USER_ID, EXTRACTED_TEXT);

    assertThat(result).isSameAs(expectedResponse);
    verify(apiClient, times(2)).call(BUILT_PROMPT);
    verify(responseParser, times(2)).parse(RAW_JSON);
  }

  @Test
  @DisplayName("재시도 후에도 파싱이 실패하면 BaseException을 던진다")
  void analyze_재시도_후_파싱실패() {
    when(promptBuilder.build(EXTRACTED_TEXT)).thenReturn(BUILT_PROMPT);
    when(apiClient.call(BUILT_PROMPT)).thenReturn(RAW_JSON);
    when(responseParser.parse(RAW_JSON))
        .thenThrow(new BaseException(ErrorCode.LLM_RESPONSE_INVALID));

    assertThatThrownBy(() -> geminiLlmService.analyze(USER_ID, EXTRACTED_TEXT))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.LLM_RESPONSE_INVALID);

    verify(apiClient, times(2)).call(BUILT_PROMPT);
    verify(responseParser, times(2)).parse(RAW_JSON);
  }

  @Test
  @DisplayName("LLM_CALL_FAILED는 재시도 없이 즉시 BaseException을 던진다")
  void analyze_API실패_즉시_예외() {
    when(promptBuilder.build(EXTRACTED_TEXT)).thenReturn(BUILT_PROMPT);
    when(apiClient.call(BUILT_PROMPT)).thenThrow(new BaseException(ErrorCode.LLM_CALL_FAILED));

    assertThatThrownBy(() -> geminiLlmService.analyze(USER_ID, EXTRACTED_TEXT))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.LLM_CALL_FAILED);

    verify(apiClient, times(1)).call(BUILT_PROMPT);
    verify(responseParser, never()).parse(Mockito.anyString());
  }
}
