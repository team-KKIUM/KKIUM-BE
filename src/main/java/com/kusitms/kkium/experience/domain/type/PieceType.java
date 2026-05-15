package com.kusitms.kkium.experience.domain.type;

public enum PieceType {
  ACTIVITY("학내외활동"),
  CAREER("인턴/직무경력"),
  EDUCATION("수강/교육"),
  ETC("기타");

  private final String label;

  PieceType(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
}
