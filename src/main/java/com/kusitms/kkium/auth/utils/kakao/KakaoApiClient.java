package com.kusitms.kkium.auth.utils.kakao;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.LOGIN_KAKAO_TOKEN_FAILED;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.LOGIN_KAKAO_USERINFO_FAILED;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.kusitms.kkium.auth.dto.response.kakao.KakaoLoginResponse;
import com.kusitms.kkium.auth.dto.response.kakao.KakaoUserInfoResponse;
import com.kusitms.kkium.global.exception.BaseException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@Component
public class KakaoApiClient {

  private final WebClient webClient;
  private static final String USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";
  private static final String TOKEN_REQUEST_URI = "https://kauth.kakao.com/oauth/token";

  @Value("${KAKAO_REST_API_KEY}")
  private String kakaoApiKey;

  @Value("${KAKAO_REDIRECT_URI}")
  private String kakaoRedirectUri;

  @Value("${KAKAO_CLIENT_SECRET}")
  private String kakaoClientSecret;

  // 인가 코드 > Access Token
  public String getAccessToken(String code) {
    String requestBody =
        "grant_type=authorization_code"
            + "&client_id="
            + kakaoApiKey
            + "&client_secret="
            + kakaoClientSecret
            + "&redirect_uri="
            + kakaoRedirectUri
            + "&code="
            + code;

    return webClient
        .post()
        .uri(TOKEN_REQUEST_URI)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .bodyValue(requestBody)
        .exchangeToMono(
            response -> {
              if (response.statusCode().is2xxSuccessful()) {
                return response.bodyToMono(KakaoLoginResponse.class);
              } else {
                return response
                    .bodyToMono(String.class)
                    .flatMap(
                        errorBody -> {
                          log.error(
                              "카카오 토큰 발급 실패 - status: {}, body: {}",
                              response.statusCode(),
                              errorBody);
                          return Mono.error(new BaseException(LOGIN_KAKAO_TOKEN_FAILED));
                        });
              }
            })
        .map(KakaoLoginResponse::accessToken)
        .block();
  }

  // Access Token > 사용자 정보 조회
  public KakaoUserInfoResponse getUserInfo(String token) {
    try {
      return webClient
          .get()
          .uri(USER_INFO_URI)
          .header("Authorization", "Bearer " + token.trim())
          .header("Content-Type", "application/json")
          .header("Accept", "application/json")
          .retrieve()
          .bodyToMono(KakaoUserInfoResponse.class)
          .block();
    } catch (WebClientResponseException e) {
      throw new BaseException(LOGIN_KAKAO_USERINFO_FAILED);
    }
  }
}
