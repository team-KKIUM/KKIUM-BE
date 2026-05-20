package com.kusitms.kkium.jd.dto.response;

import java.util.List;

public record JdQuestionExperienceResponse(List<ExperienceMatchItem> experiences) {

  public record ExperienceMatchItem(
      Long experienceId, String title, String oneLineIntro, int usageFitScore) {}
}
