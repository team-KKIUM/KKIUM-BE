package com.kusitms.kkium.notion.utils;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.NOTION_TOKEN_FAILED;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.notion.dto.response.NotionTokenResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@Component
public class NotionApiClient {

  private final WebClient webClient;

  private static final String TOKEN_URI = "https://api.notion.com/v1/oauth/token";

  @Value("${notion.client-id}")
  private String clientId;

  @Value("${notion.client-secret}")
  private String clientSecret;

  @Value("${notion.redirect-uri}")
  private String redirectUri;

  // 인가 코드 → access_token 교환
  public NotionTokenResponse getAccessToken(String code) {
    String credentials =
        Base64.getEncoder()
            .encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

    return webClient
        .post()
        .uri(TOKEN_URI)
        .header("Authorization", "Basic " + credentials)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            Map.of(
                "grant_type", "authorization_code",
                "code", code,
                "redirect_uri", redirectUri))
        .exchangeToMono(
            response -> {
              if (response.statusCode().is2xxSuccessful()) {
                return response.bodyToMono(NotionTokenResponse.class);
              } else {
                return response
                    .bodyToMono(String.class)
                    .flatMap(
                        errorBody -> {
                          log.error(
                              "Notion 토큰 발급 실패 - status: {}, body: {}",
                              response.statusCode(),
                              errorBody);
                          return Mono.error(new BaseException(NOTION_TOKEN_FAILED));
                        });
              }
            })
        .block();
  }

  // OAuth 인증 URL 생성
  public String getAuthorizationUrl(String state) {
    return UriComponentsBuilder.fromUriString("https://api.notion.com/v1/oauth/authorize")
        .queryParam("client_id", clientId)
        .queryParam("response_type", "code")
        .queryParam("owner", "user")
        .queryParam("redirect_uri", redirectUri)
        .queryParam("state", state)
        .build()
        .toUriString();
  }
}
