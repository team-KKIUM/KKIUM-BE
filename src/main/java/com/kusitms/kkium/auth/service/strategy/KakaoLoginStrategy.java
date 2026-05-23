package com.kusitms.kkium.auth.service.strategy;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.LOGIN_KAKAO_USERINFO_FAILED;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.USER_ALREADY_EXISTS;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.kusitms.kkium.auth.dto.response.LoginResponse;
import com.kusitms.kkium.auth.dto.response.kakao.KakaoUserInfoResponse;
import com.kusitms.kkium.auth.utils.JwtTokenProvider;
import com.kusitms.kkium.auth.utils.kakao.KakaoApiClient;
import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.user.domain.User;
import com.kusitms.kkium.user.domain.type.LoginType;
import com.kusitms.kkium.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service("KAKAO")
@RequiredArgsConstructor
public class KakaoLoginStrategy implements SocialLoginStrategy {

  private final UserRepository userRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final KakaoApiClient kakaoApiClient;

  @Override
  public LoginResponse login(String code) {
    String kakaoAccessToken = kakaoApiClient.getAccessToken(code);
    KakaoUserInfoResponse userInfo = kakaoApiClient.getUserInfo(kakaoAccessToken);

    String socialId = String.valueOf(userInfo.id());
    String email = userInfo.kakaoAccount() != null ? userInfo.kakaoAccount().email() : null;
    String name = resolveName(userInfo);

    LocalDateTime now = LocalDateTime.now();
    User user =
        userRepository
            .findBySocialIdAndLoginType(socialId, LoginType.KAKAO)
            .map(existingUser -> resolveDeletedUser(existingUser, name, socialId, email, now))
            .orElseGet(
                () -> {
                  anonymizeExpiredDeletedUserByEmail(email, now);
                  return saveKakaoUser(name, socialId, email);
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
    return saveKakaoUser(name, socialId, email);
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

  private User saveKakaoUser(String name, String socialId, String email) {
    return userRepository.save(
        User.socialLoginBuilder()
            .name(name)
            .socialId(socialId)
            .loginType(LoginType.KAKAO)
            .email(email)
            .build());
  }

  private String resolveName(KakaoUserInfoResponse userInfo) {
    if (userInfo.properties() == null
        || userInfo.properties().nickname() == null
        || userInfo.properties().nickname().isBlank()) {
      throw new BaseException(LOGIN_KAKAO_USERINFO_FAILED);
    }
    return userInfo.properties().nickname().trim();
  }
}
