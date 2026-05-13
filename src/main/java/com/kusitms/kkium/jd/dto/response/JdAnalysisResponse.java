package com.kusitms.kkium.jd.dto.response;

import com.kusitms.kkium.jd.domain.Jd;
import com.kusitms.kkium.jd.domain.type.AnalysisStatus;

public record JdAnalysisResponse(
    Long jdId,
    AnalysisStatus analysisStatus,
    String hardSkill,
    String softSkill,
    String mainResponsibilities,
    String requiredQualifications,
    String preferredQualifications) {

  public static JdAnalysisResponse from(Jd jd) {
    return new JdAnalysisResponse(
        jd.getId(),
        jd.getAnalysisStatus(),
        jd.getHardSkill(),
        jd.getSoftSkill(),
        jd.getMainResponsibilities(),
        jd.getRequiredQualifications(),
        jd.getPreferredQualifications());
  }
}
