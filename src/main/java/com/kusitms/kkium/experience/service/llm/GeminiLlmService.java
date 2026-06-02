package com.kusitms.kkium.experience.service.llm;

import org.springframework.stereotype.Service;

import com.kusitms.kkium.experience.dto.response.ExperienceAnalyzeResponse;
import com.kusitms.kkium.experience.service.llm.pipeline.ExperiencePromptBuilder;
import com.kusitms.kkium.experience.service.llm.pipeline.ExperienceResponseParser;
import com.kusitms.kkium.experience.service.llm.pipeline.GeminiApiClient;
import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.global.exception.errorcode.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiLlmService implements LlmService {

  private final ExperiencePromptBuilder promptBuilder;
  private final GeminiApiClient apiClient;
  private final ExperienceResponseParser responseParser;

  @Override
  public ExperienceAnalyzeResponse analyze(Long userId, String extractedText) {
    String prompt = promptBuilder.build(extractedText);

    try {
      return callAndParse(prompt);
    } catch (BaseException e) {
      if (!isParsingError(e)) {
        throw e;
      }
      log.warn("응답 파싱 실패, 1회 재시도: {}", e.getErrorCode());
      return callAndParse(prompt);
    }
  }

  private ExperienceAnalyzeResponse callAndParse(String prompt) {
    String rawJson = apiClient.call(prompt);
    return responseParser.parse(rawJson);
  }

  private boolean isParsingError(BaseException e) {
    ErrorCode code = e.getErrorCode();
    return code == ErrorCode.LLM_RESPONSE_INVALID || code == ErrorCode.LLM_SCHEMA_VIOLATION;
  }
}
