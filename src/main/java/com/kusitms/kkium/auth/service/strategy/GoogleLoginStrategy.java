package com.kusitms.kkium.auth.service.strategy;

import org.springframework.stereotype.Service;

import com.kusitms.kkium.auth.dto.response.LoginResponse;
import com.kusitms.kkium.auth.dto.response.google.GoogleUserInfoResponse;
import com.kusitms.kkium.auth.utils.JwtTokenProvider;
import com.kusitms.kkium.auth.utils.google.GoogleApiClient;
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
    String name = userInfo.name() != null && !userInfo.name().isBlank() ? userInfo.name() : "구글 유저";

    User user =
        userRepository
            .findBySocialIdAndLoginType(socialId, LoginType.GOOGLE)
            .orElseGet(
                () ->
                    userRepository.save(
                        User.socialLoginBuilder()
                            .name(name)
                            .socialId(socialId)
                            .loginType(LoginType.GOOGLE)
                            .email(email)
                            .build()));

    String token = jwtTokenProvider.createToken(user.getId().toString());
    return LoginResponse.from(user.getName(), user.getRole(), token);
  }
}
