package com.kusitms.kkium.auth.utils.google;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.LOGIN_GOOGLE_TOKEN_FAILED;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.LOGIN_GOOGLE_USERINFO_FAILED;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.kusitms.kkium.auth.dto.response.google.GoogleLoginResponse;
import com.kusitms.kkium.auth.dto.response.google.GoogleUserInfoResponse;
import com.kusitms.kkium.global.exception.BaseException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@Component
public class GoogleApiClient {

  private final WebClient webClient;
  private static final String USER_INFO_URI = "https://www.googleapis.com/oauth2/v1/userinfo";
  private static final String TOKEN_REQUEST_URI = "https://oauth2.googleapis.com/token";

  @Value("${google.client-id}")
  private String googleClientId;

  @Value("${google.redirect-uri}")
  private String googleRedirectUri;

  @Value("${google.client-secret}")
  private String googleClientSecret;

  public String getAccessToken(String code) {
    String requestBody =
        "grant_type=authorization_code"
            + "&client_id="
            + googleClientId
            + "&client_secret="
            + googleClientSecret
            + "&redirect_uri="
            + googleRedirectUri
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
                return response.bodyToMono(GoogleLoginResponse.class);
              }
              return response
                  .bodyToMono(String.class)
                  .flatMap(
                      errorBody -> {
                        log.error(
                            "구글 토큰 발급 실패 - status: {}, body: {}", response.statusCode(), errorBody);
                        return Mono.error(new BaseException(LOGIN_GOOGLE_TOKEN_FAILED));
                      });
            })
        .map(GoogleLoginResponse::accessToken)
        .block();
  }

  public GoogleUserInfoResponse getUserInfo(String token) {
    try {
      return webClient
          .get()
          .uri(USER_INFO_URI)
          .header("Authorization", "Bearer " + token.trim())
          .header("Content-Type", "application/json")
          .header("Accept", "application/json")
          .retrieve()
          .bodyToMono(GoogleUserInfoResponse.class)
          .block();
    } catch (WebClientResponseException e) {
      throw new BaseException(LOGIN_GOOGLE_USERINFO_FAILED);
    }
  }
}
