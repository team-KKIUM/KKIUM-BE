package com.kusitms.kkium.jd.utils.llm.result;

import java.util.List;

public record LlmExperienceDetailResult(
    String strengths,
    String weaknesses,
    String usageGuide,
    List<HighlightKeyword> highlightKeywords) {

  public static LlmExperienceDetailResult empty() {
    return new LlmExperienceDetailResult("분석에 실패했습니다.", "분석에 실패했습니다.", "분석에 실패했습니다.", List.of());
  }
}
