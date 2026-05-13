package com.kusitms.kkium.jd.dto.response;

import java.util.List;

import com.kusitms.kkium.jd.utils.llm.LlmJdParser;

public record JdFetchResponse(
    String url,
    String postingTitle,
    String companyName,
    String recruitmentField,
    String startDate,
    String endDate,
    List<String> questions,
    String content) {

  public static JdFetchResponse from(LlmJdParser.ParsedJd parsed, String url) {
    return new JdFetchResponse(
        url,
        parsed.title(),
        parsed.companyName(),
        parsed.recruitmentField(),
        parsed.startDate(),
        parsed.endDate(),
        parsed.questions(),
        parsed.content());
  }
}
