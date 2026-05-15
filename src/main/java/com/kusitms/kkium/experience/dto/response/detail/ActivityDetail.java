package com.kusitms.kkium.experience.dto.response.detail;

import java.time.LocalDate;

import com.kusitms.kkium.experience.domain.Activity;

public record ActivityDetail(
    String name,
    Integer teamNum,
    String role,
    Integer contributionRate,
    LocalDate startDate,
    LocalDate endDate) {

  public static ActivityDetail from(Activity activity) {
    return new ActivityDetail(
        activity.getName(),
        activity.getTeamNum(),
        activity.getRole(),
        activity.getContributionRate(),
        activity.getStartDate(),
        activity.getEndDate());
  }
}
