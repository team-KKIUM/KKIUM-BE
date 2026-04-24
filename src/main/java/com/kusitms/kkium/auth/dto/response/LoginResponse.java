package com.kusitms.kkium.auth.dto.response;

import com.kusitms.kkium.user.domain.type.Role;

public record LoginResponse(String name, Role role, String accessToken) {

  public static LoginResponse from(String name, Role role, String accessToken) {
    return new LoginResponse(name, role, accessToken);
  }
}
