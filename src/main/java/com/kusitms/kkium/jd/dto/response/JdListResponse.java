package com.kusitms.kkium.jd.dto.response;

import java.time.LocalDateTime;

import com.kusitms.kkium.jd.domain.Jd;

public record JdListResponse(
    Long id,
    String title,
    String companyName,
    String recruitmentField,
    LocalDateTime startDate,
    LocalDateTime endDate,
    Boolean isTarget,
    Integer sortOrder) {

  public static JdListResponse from(Jd jd) {
    return new JdListResponse(
        jd.getId(),
        jd.getTitle(),
        jd.getCompanyName(),
        jd.getRecruitmentField(),
        jd.getStartDate(),
        jd.getEndDate(),
        jd.getIsTarget(),
        jd.getSortOrder());
  }
}
