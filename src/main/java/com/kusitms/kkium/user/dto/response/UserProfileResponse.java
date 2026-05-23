package com.kusitms.kkium.user.dto.response;

import com.kusitms.kkium.user.domain.User;

public record UserProfileResponse(String name, String email) {

  public static UserProfileResponse from(User user) {
    return new UserProfileResponse(user.getName(), user.getEmail());
  }
}
