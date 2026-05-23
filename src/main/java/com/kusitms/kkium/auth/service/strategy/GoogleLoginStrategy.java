package com.kusitms.kkium.auth.service.strategy;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.LOGIN_GOOGLE_USERINFO_FAILED;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.USER_ALREADY_EXISTS;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.kusitms.kkium.auth.dto.response.LoginResponse;
import com.kusitms.kkium.auth.dto.response.google.GoogleUserInfoResponse;
import com.kusitms.kkium.auth.utils.JwtTokenProvider;
import com.kusitms.kkium.auth.utils.google.GoogleApiClient;
import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.user.domain.User;
import com.kusitms.kkium.user.domain.type.LoginType;
import com.kusitms.kkium.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service("GOOGLE")
@RequiredArgsConstructor
public class GoogleLoginStrategy implements SocialLoginStrategy {

  private final UserRepository userRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final GoogleApiClient googleApiClient;

  @Override
  public LoginResponse login(String code) {
    String googleAccessToken = googleApiClient.getAccessToken(code);
    GoogleUserInfoResponse userInfo = googleApiClient.getUserInfo(googleAccessToken);

    String socialId = userInfo.id();
    String email = userInfo.email();
    String name = resolveName(userInfo);

    LocalDateTime now = LocalDateTime.now();
    User user =
        userRepository
            .findBySocialIdAndLoginType(socialId, LoginType.GOOGLE)
            .map(existingUser -> resolveDeletedUser(existingUser, name, socialId, email, now))
            .orElseGet(
                () -> {
                  anonymizeExpiredDeletedUserByEmail(email, now);
                  return saveGoogleUser(name, socialId, email);
                });

    String token = jwtTokenProvider.createToken(user.getId().toString());
    return LoginResponse.from(user.getName(), user.getRole(), token);
  }

  private User resolveDeletedUser(
      User user, String name, String socialId, String email, LocalDateTime now) {
    if (user.getDeleteAt() == null) {
      return user;
    }
    if (user.canRestore(now)) {
      user.restore();
      return user;
    }
    user.anonymizeDeletedAccount(now);
    userRepository.flush();
    return saveGoogleUser(name, socialId, email);
  }

  private void anonymizeExpiredDeletedUserByEmail(String email, LocalDateTime now) {
    if (email == null || email.isBlank()) {
      return;
    }
    userRepository
        .findByEmail(email)
        .ifPresent(
            user -> {
              if (user.isRestorePeriodExpired(now)) {
                user.anonymizeDeletedAccount(now);
                userRepository.flush();
                return;
              }
              throw new BaseException(USER_ALREADY_EXISTS);
            });
  }

  private User saveGoogleUser(String name, String socialId, String email) {
    return userRepository.save(
        User.socialLoginBuilder()
            .name(name)
            .socialId(socialId)
            .loginType(LoginType.GOOGLE)
            .email(email)
            .build());
  }

  private String resolveName(GoogleUserInfoResponse userInfo) {
    if (userInfo.name() == null || userInfo.name().isBlank()) {
      throw new BaseException(LOGIN_GOOGLE_USERINFO_FAILED);
    }
    return userInfo.name().trim();
  }
}
