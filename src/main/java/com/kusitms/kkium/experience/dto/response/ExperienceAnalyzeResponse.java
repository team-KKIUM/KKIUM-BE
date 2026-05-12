package com.kusitms.kkium.experience.dto.response;

import java.time.LocalDate;

public record ExperienceAnalyzeResponse(
    // 공통 (Experience 테이블)
    String title,
    String oneLineIntro,

    // 유형별 기본정보 (전부 채워서 반환, 사용자가 Step2에서 유형 선택 시 해당 필드만 사용)
    ActivityInfo activityInfo,
    CareerInfo careerInfo,
    EducationInfo educationInfo,

    // STAR (Experience 테이블)
    String situation,
    String task,
    String act,
    String result,
    String taken) {

  // Activity 테이블
  public record ActivityInfo(
      String name,
      Integer teamNum,
      LocalDate startDate,
      LocalDate endDate,
      Integer contributionRate,
      String role) {}

  // Career 테이블
  public record CareerInfo(
      String name,
      String company,
      String employmentStatus,
      LocalDate startDate,
      LocalDate endDate) {}

  // Education 테이블
  public record EducationInfo(
      String organizationName,
      String name,
      LocalDate startDate,
      LocalDate endDate) {}
}
