package com.kusitms.kkium.experience.service.llm;

import org.springframework.stereotype.Service;

import com.kusitms.kkium.experience.dto.response.ExperienceAnalyzeResponse;
import com.kusitms.kkium.experience.service.llm.pipeline.ExperiencePromptBuilder;
import com.kusitms.kkium.experience.service.llm.pipeline.ExperienceResponseParser;
import com.kusitms.kkium.experience.service.llm.pipeline.GeminiApiClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GeminiLlmService implements LlmService {

  private final ExperiencePromptBuilder promptBuilder;
  private final GeminiApiClient apiClient;
  private final ExperienceResponseParser responseParser;

  @Override
  public ExperienceAnalyzeResponse analyze(Long userId, String extractedText) {
    String prompt = promptBuilder.build(extractedText);
    String rawJson = apiClient.call(prompt);
    return responseParser.parse(rawJson);
  }
}
