package com.kusitms.kkium.experience.domain.type;

public enum TagCategory {
  TECH("기술"),
  COMPETENCY("역량");

  private final String label;

  TagCategory(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
}
