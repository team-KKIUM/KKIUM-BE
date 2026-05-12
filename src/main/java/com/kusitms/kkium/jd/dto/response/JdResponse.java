package com.kusitms.kkium.jd.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kusitms.kkium.jd.domain.Jd;

public record JdResponse(
    String postingTitle,
    String companyName,
    String recruitmentField,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate,
    List<JdQuestionResponse> questions) {

  public static JdResponse from(Jd jd, List<JdQuestionResponse> questions) {
    return new JdResponse(
        jd.getPostingTitle(),
        jd.getCompanyName(),
        jd.getRecruitmentField(),
        jd.getStartDate(),
        jd.getEndDate(),
        questions);
  }
}
