package com.kusitms.kkium.experience.dto.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ExperienceUpdateRequest(
    @NotBlank String title,
    @NotBlank String oneLineIntro,
    @NotNull List<TagCreateRequest> tags,
    String situation,
    String task,
    String act,
    String result,
    String taken,
    @NotNull Detail detail) {

  public record Detail(
      // ACTIVITY, EDUCATION 공통
      String name,

      // ACTIVITY 전용
      Integer teamNum,
      String role,
      Integer contributionRate,

      // CAREER 전용
      String company,
      String employmentStatus,

      // EDUCATION 전용
      String organizationName,

      // 공통 (기간)
      @NotNull LocalDate startDate,
      @NotNull LocalDate endDate) {}
}
