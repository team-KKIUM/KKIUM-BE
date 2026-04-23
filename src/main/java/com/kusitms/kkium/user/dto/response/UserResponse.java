package com.kusitms.kkium.user.dto.response;

import com.kusitms.kkium.user.domain.User;

public record UserResponse(Long id, String name) {
  public static UserResponse from(User user) {
    return new UserResponse(user.getId(), user.getName());
  }
}
