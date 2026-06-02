package com.kusitms.kkium.jd.utils.llm.result;

import java.util.List;

public record LlmWritingGuideResult(
    List<String> coreKeywords, String connectionToJd, String writingGuide) {

  public static LlmWritingGuideResult empty() {
    return new LlmWritingGuideResult(List.of(), "분석에 실패했습니다.", "분석에 실패했습니다.");
  }
}
