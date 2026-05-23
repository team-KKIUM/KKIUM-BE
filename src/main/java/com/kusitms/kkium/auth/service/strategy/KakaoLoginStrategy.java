package com.kusitms.kkium.auth.service.strategy;

import org.springframework.stereotype.Service;

import com.kusitms.kkium.auth.dto.response.LoginResponse;
import com.kusitms.kkium.auth.dto.response.kakao.KakaoUserInfoResponse;
import com.kusitms.kkium.auth.utils.JwtTokenProvider;
import com.kusitms.kkium.auth.utils.kakao.KakaoApiClient;
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
    String name = userInfo.properties() != null ? userInfo.properties().nickname() : "카카오 유저";

    User user =
        userRepository
            .findBySocialIdAndLoginType(socialId, LoginType.KAKAO)
            .orElseGet(
                () ->
                    userRepository.save(
                        User.socialLoginBuilder()
                            .name(name)
                            .socialId(socialId)
                            .loginType(LoginType.KAKAO)
                            .email(email)
                            .build()));

    String token = jwtTokenProvider.createToken(user.getId().toString());
    return LoginResponse.from(user.getName(), user.getRole(), token);
  }
}
