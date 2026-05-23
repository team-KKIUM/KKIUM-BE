package com.kusitms.kkium.auth.service.strategy;

import com.kusitms.kkium.auth.dto.response.LoginResponse;

public interface SocialLoginStrategy {
  LoginResponse login(String code);
}
