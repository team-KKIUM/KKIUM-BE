package com.kusitms.kkium.auth.dto.response;

import com.kusitms.kkium.user.domain.type.Role;

public record LoginResponse(String name, Role role, String accessToken, boolean termsAgreed) {

  public static LoginResponse from(
      String name, Role role, String accessToken, boolean termsAgreed) {
    return new LoginResponse(name, role, accessToken, termsAgreed);
  }
}
