package com.kusitms.kkium.jd.dto.response;

import java.util.List;

import com.kusitms.kkium.experience.domain.type.PieceType;
import com.kusitms.kkium.experience.dto.response.TagResponse;
import com.kusitms.kkium.jd.domain.type.AnalysisStatus;

public record JdMatchAnalysisResponse(
    AnalysisStatus analysisStatus, JdInfo jdInfo, MatchResult matchResult) {

  public record JdInfo(
      String companyName,
      String recruitmentField,
      String startDate,
      String endDate,
      List<String> hardSkills,
      List<String> softSkills,
      String mainResponsibilities,
      String requiredQualifications,
      String preferredQualifications) {}

  public record MatchResult(
      int applicationFitScore, int usableExperienceCount, List<ExperienceMatchCard> experiences) {}

  public record ExperienceMatchCard(
      Long pieceId,
      Long experienceId,
      PieceType type,
      String title,
      String oneLineIntro,
      List<TagResponse> tags,
      int usageFitScore) {}
}
