package com.kusitms.kkium.experience.dto.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExperienceUpdateRequest(
    @NotBlank @Size(max = 80, message = "제목은 80자 이하여야 합니다.") String title,
    @NotBlank @Size(max = 100, message = "한 줄 소개는 100자 이하여야 합니다.") String oneLineIntro,
    @NotNull @Valid List<TagCreateRequest> tags,
    @Size(max = 1000, message = "Situation은 1000자 이하여야 합니다.") String situation,
    @Size(max = 1000, message = "Task는 1000자 이하여야 합니다.") String task,
    @Size(max = 1000, message = "Action은 1000자 이하여야 합니다.") String act,
    @Size(max = 1000, message = "Result는 1000자 이하여야 합니다.") String result,
    @Size(max = 1000, message = "Taken은 1000자 이하여야 합니다.") String taken,
    @NotNull @Valid Detail detail) {

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
