package com.kusitms.kkium.experience.dto.response.detail;

import java.time.LocalDate;

import com.kusitms.kkium.experience.domain.Career;

public record CareerDetail(
    String name,
    String company,
    String employmentStatus,
    LocalDate startDate,
    LocalDate endDate) {

  public static CareerDetail from(Career career) {
    return new CareerDetail(
        career.getName(),
        career.getCompany(),
        career.getEmploymentStatus(),
        career.getStartDate(),
        career.getEndDate());
  }
}
