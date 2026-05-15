package com.kusitms.kkium.experience.dto.response.detail;

import java.time.LocalDate;

import com.kusitms.kkium.experience.domain.Education;

public record EducationDetail(
    String organizationName, String name, LocalDate startDate, LocalDate endDate) {

  public static EducationDetail from(Education education) {
    return new EducationDetail(
        education.getOrganizationName(),
        education.getName(),
        education.getStartDate(),
        education.getEndDate());
  }
}
