package com.kusitms.kkium.user.domain.type;

public enum Role {
  ROLE_USER("유저"),
  ROLE_ADMIN("관리자");

  private final String label;

  Role(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
}
