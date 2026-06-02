package com.kusitms.kkium.auth.service.strategy;

import com.kusitms.kkium.auth.domain.type.RedirectType;
import com.kusitms.kkium.auth.dto.response.LoginResponse;

public interface SocialLoginStrategy {
  LoginResponse login(String code);

  default LoginResponse login(String code, RedirectType redirectType) {
    return login(code);
  }
}
