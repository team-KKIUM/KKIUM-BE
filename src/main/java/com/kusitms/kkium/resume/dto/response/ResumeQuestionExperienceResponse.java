package com.kusitms.kkium.resume.dto.response;

import java.time.LocalDate;
import java.util.List;

public record ResumeQuestionExperienceResponse(List<ExperienceMatchItem> experiences) {

  public record ExperienceMatchItem(
      Long experienceId,
      String title,
      String oneLineIntro,
      LocalDate startDate,
      LocalDate endDate,
      int usageFitScore) {}
}
