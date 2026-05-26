package com.kusitms.kkium.jd.dto.response;

import java.util.List;

import com.kusitms.kkium.jd.utils.llm.LlmMatchScoreService.HighlightKeyword;

public record JdExperienceAnalysisResponse(Long experienceId, ExperienceAnalysis analysis) {

  public record ExperienceAnalysis(
      String strengths,
      String weaknesses,
      String usageGuide,
      List<HighlightKeyword> highlightKeywords) {}
}
