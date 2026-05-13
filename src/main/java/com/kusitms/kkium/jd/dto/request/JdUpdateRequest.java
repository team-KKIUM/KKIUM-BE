package com.kusitms.kkium.jd.dto.request;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

public record JdUpdateRequest(
    String postingTitle,
    String companyName,
    String recruitmentField,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate,
    List<QuestionUpdateRequest> questions) {

  public record QuestionUpdateRequest(Long questionId, String content, String answer) {}
}
