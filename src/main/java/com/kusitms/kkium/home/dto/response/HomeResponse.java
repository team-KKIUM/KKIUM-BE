package com.kusitms.kkium.home.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.kusitms.kkium.experience.domain.type.PieceType;

public record HomeResponse(
    List<TargetJdInfo> targetJds,
    int totalExperienceCount,
    int thisMonthExperienceCount,
    int lastMonthDiff,
    JobTypeInfo jobType,
    List<ExperienceDistribution> experienceDistribution) {

  public record TargetJdInfo(
      Long jdId,
      String companyName,
      String recruitmentField,
      LocalDateTime startDate,
      LocalDateTime endDate,
      List<String> hardSkills,
      List<String> softSkills,
      Integer applicationFitScore) {}

  public record JobTypeInfo(String typeName) {}

  public record ExperienceDistribution(PieceType type, int count, int percentage) {}
}
