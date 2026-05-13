package com.kusitms.kkium.jd.domain.type;

public enum AnalysisStatus {
  PENDING("분석 중"),
  COMPLETED("분석 완료"),
  FAILED("분석 실패");

  private final String label;

  AnalysisStatus(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
}
