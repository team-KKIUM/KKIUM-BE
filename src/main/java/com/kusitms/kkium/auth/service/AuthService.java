package com.kusitms.kkium.auth.service;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.INVALID_CREDENTIALS;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.INVALID_INPUT_VALUE;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.USER_ALREADY_EXISTS;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.USER_NOT_FOUND;

import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kusitms.kkium.auth.dto.request.BasicLoginRequest;
import com.kusitms.kkium.auth.dto.request.BasicSignupRequest;
import com.kusitms.kkium.auth.dto.response.LoginResponse;
import com.kusitms.kkium.auth.service.strategy.SocialLoginStrategy;
import com.kusitms.kkium.auth.utils.JwtTokenProvider;
import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.user.domain.User;
import com.kusitms.kkium.user.domain.type.LoginType;
import com.kusitms.kkium.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final PasswordEncoder passwordEncoder;
  private final Map<String, SocialLoginStrategy> loginStrategyMap;

  @Transactional
  public void signup(BasicSignupRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new BaseException(USER_ALREADY_EXISTS);
    }
    userRepository.save(
        User.basicLoginBuilder()
            .name(request.name())
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .build());
  }

  @Transactional(readOnly = true)
  public LoginResponse login(BasicLoginRequest request) {
    User user =
        userRepository
            .findByEmail(request.email())
            .orElseThrow(() -> new BaseException(USER_NOT_FOUND));

    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
      throw new BaseException(INVALID_CREDENTIALS);
    }

    String token = jwtTokenProvider.createToken(user.getId().toString());
    return LoginResponse.from(user.getName(), user.getRole(), token);
  }

  @Transactional
  public LoginResponse socialLogin(LoginType loginType, String code) {
    SocialLoginStrategy loginStrategy = loginStrategyMap.get(loginType.name());
    if (loginStrategy == null) {
      throw new BaseException(INVALID_INPUT_VALUE);
    }
    return loginStrategy.login(code);
  }
}
