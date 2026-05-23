package com.kusitms.kkium.user.domain.type;

public enum LoginType {
  KAKAO("카카오"),
  GOOGLE("구글");

  private final String label;

  LoginType(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
}
