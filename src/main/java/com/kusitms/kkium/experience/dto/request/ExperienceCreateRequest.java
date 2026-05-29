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

  @AssertTrue(message = "학내외활동 활동명은 필수입니다.")
  public boolean isActivityNameValid() {
    return type != PieceType.ACTIVITY || hasText(name);
  }

  @AssertTrue(message = "학내외활동 팀 인원은 필수입니다.")
  public boolean isActivityTeamNumValid() {
    return type != PieceType.ACTIVITY || teamNum != null;
  }

  @AssertTrue(message = "학내외활동 역할은 필수입니다.")
  public boolean isActivityRoleValid() {
    return type != PieceType.ACTIVITY || hasText(role);
  }

  @AssertTrue(message = "학내외활동 기여도는 필수입니다.")
  public boolean isActivityContributionRateValid() {
    return type != PieceType.ACTIVITY || contributionRate != null;
  }

  @AssertTrue(message = "인턴/직무경력 회사/기관/단체명은 필수이며 50자 이하여야 합니다.")
  public boolean isCareerCompanyValid() {
    return type != PieceType.CAREER || (hasText(company) && company.length() <= 50);
  }

  @AssertTrue(message = "인턴/직무경력 고용 형태는 필수입니다.")
  public boolean isCareerEmploymentStatusValid() {
    return type != PieceType.CAREER || hasText(employmentStatus);
  }

  @AssertTrue(message = "수강/교육 교육기관명은 필수이며 50자 이하여야 합니다.")
  public boolean isEducationOrganizationNameValid() {
    return type != PieceType.EDUCATION
        || (hasText(organizationName) && organizationName.length() <= 50);
  }

  @AssertTrue(message = "수강/교육 수강명은 필수이며 80자 이하여야 합니다.")
  public boolean isEducationNameValid() {
    return type != PieceType.EDUCATION || (hasText(name) && name.length() <= 80);
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
