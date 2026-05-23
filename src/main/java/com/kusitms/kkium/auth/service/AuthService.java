package com.kusitms.kkium.auth.service;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.INVALID_CREDENTIALS;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.INVALID_INPUT_VALUE;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.USER_ALREADY_EXISTS;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.USER_NOT_FOUND;

import java.time.LocalDateTime;
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
    userRepository
        .findByEmail(request.email())
        .ifPresent(
            user -> {
              LocalDateTime now = LocalDateTime.now();
              if (user.isRestorePeriodExpired(now)) {
                user.anonymizeDeletedAccount(now);
                return;
              }
              throw new BaseException(USER_ALREADY_EXISTS);
            });
    userRepository.flush();
    userRepository.save(
        User.basicLoginBuilder()
            .name(request.name())
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .build());
  }

  @Transactional
  public LoginResponse login(BasicLoginRequest request) {
    User user =
        userRepository
            .findByEmail(request.email())
            .orElseThrow(() -> new BaseException(USER_NOT_FOUND));

    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
      throw new BaseException(INVALID_CREDENTIALS);
    }

    if (user.getDeleteAt() != null) {
      LocalDateTime now = LocalDateTime.now();
      if (!user.canRestore(now)) {
        user = recreateBasicUser(user, request, now);
      } else {
        user.restore();
      }
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

  private User recreateBasicUser(User deletedUser, BasicLoginRequest request, LocalDateTime now) {
    String name = deletedUser.getName();
    deletedUser.anonymizeDeletedAccount(now);
    userRepository.flush();
    return userRepository.save(
        User.basicLoginBuilder()
            .name(name)
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .build());
  }
}
