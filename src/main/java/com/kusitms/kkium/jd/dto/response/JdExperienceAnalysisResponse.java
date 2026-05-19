package com.kusitms.kkium.jd.dto.response;

import java.util.List;

public record JdExperienceAnalysisResponse(Long experienceId, ExperienceAnalysis analysis) {

  public record ExperienceAnalysis(
      String strengths, String weaknesses, String usageGuide, List<String> highlightKeywords) {}
}
