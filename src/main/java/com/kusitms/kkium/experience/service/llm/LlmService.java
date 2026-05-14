package com.kusitms.kkium.experience.service.llm;

import com.kusitms.kkium.experience.dto.response.ExperienceAnalyzeResponse;

public interface LlmService {
  ExperienceAnalyzeResponse analyze(Long userId, String extractedText);
}
