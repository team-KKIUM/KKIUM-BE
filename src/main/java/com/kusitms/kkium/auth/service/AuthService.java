package com.kusitms.kkium.auth.service;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.INVALID_CREDENTIALS;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.USER_ALREADY_EXISTS;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.USER_NOT_FOUND;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kusitms.kkium.auth.dto.request.BasicLoginRequest;
import com.kusitms.kkium.auth.dto.request.BasicSignupRequest;
import com.kusitms.kkium.auth.dto.response.LoginResponse;
import com.kusitms.kkium.auth.dto.response.kakao.KakaoUserInfoResponse;
import com.kusitms.kkium.auth.utils.JwtTokenProvider;
import com.kusitms.kkium.auth.utils.kakao.KakaoApiClient;
import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.user.domain.User;
import com.kusitms.kkium.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final PasswordEncoder passwordEncoder;
  private final KakaoApiClient kakaoApiClient;

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
  public LoginResponse kakaoLogin(String code) {
    String kakaoAccessToken = kakaoApiClient.getAccessToken(code);
    KakaoUserInfoResponse userInfo = kakaoApiClient.getUserInfo(kakaoAccessToken);

    Long kakaoId = userInfo.id();
    String email = userInfo.kakaoAccount() != null ? userInfo.kakaoAccount().email() : null;
    String name = userInfo.properties() != null ? userInfo.properties().nickname() : "카카오 유저";

    User user =
        userRepository
            .findByKakaoId(kakaoId)
            .orElseGet(
                () ->
                    userRepository.save(
                        User.kakaoLoginBuilder().name(name).kakaoId(kakaoId).email(email).build()));

    String token = jwtTokenProvider.createToken(user.getId().toString());
    return LoginResponse.from(user.getName(), user.getRole(), token);
  }
}
