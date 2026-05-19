package com.kusitms.kkium.jd.dto.request;

import java.time.LocalDateTime;
import java.util.List;

public record JdUpdateRequest(
    String postingTitle,
    String companyName,
    String recruitmentField,
    LocalDateTime startDate,
    LocalDateTime endDate,
    List<QuestionUpdateRequest> questions) {

  public record QuestionUpdateRequest(Long questionId, String content, String answer) {}
}
