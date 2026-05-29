package com.kusitms.kkium.experience.dto.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.kusitms.kkium.experience.domain.type.PieceType;

public record ExperienceCreateRequest(
    @NotNull PieceType type,
    @NotBlank @Size(max = 80, message = "제목은 80자 이하여야 합니다.") String title,
    @NotBlank @Size(max = 200, message = "한 줄 설명은 200자 이하여야 합니다.") String oneLineIntro,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    @NotBlank String situation,
    @NotBlank String task,
    @NotBlank String act,
    @NotBlank String result,
    @NotBlank String taken,
    @NotNull @Valid List<TagCreateRequest> tags,

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
    String organizationName) {

  @AssertTrue(message = "인턴/직무경력 회사/기관/단체명은 50자 이하여야 합니다.")
  public boolean isCareerCompanyLengthValid() {
    return type != PieceType.CAREER || company == null || company.length() <= 50;
  }

  @AssertTrue(message = "수강/교육 교육기관명은 50자 이하여야 합니다.")
  public boolean isEducationOrganizationNameLengthValid() {
    return type != PieceType.EDUCATION
        || organizationName == null
        || organizationName.length() <= 50;
  }

  @AssertTrue(message = "수강/교육 수강명은 80자 이하여야 합니다.")
  public boolean isEducationNameLengthValid() {
    return type != PieceType.EDUCATION || name == null || name.length() <= 80;
  }
}
