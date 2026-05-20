package com.kusitms.kkium.jd.dto.response;

import java.time.LocalDate;
import java.util.List;

public record JdQuestionExperienceResponse(List<ExperienceMatchItem> experiences) {

  public record ExperienceMatchItem(
      Long experienceId,
      String title,
      String oneLineIntro,
      LocalDate startDate,
      LocalDate endDate,
      int usageFitScore) {}
}
