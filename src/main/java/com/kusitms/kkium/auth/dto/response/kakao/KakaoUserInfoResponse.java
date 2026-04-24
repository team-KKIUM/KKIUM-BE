package com.kusitms.kkium.auth.dto.response.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoUserInfoResponse(
    Long id, @JsonProperty("kakao_account") KakaoAccount kakaoAccount, Properties properties) {

  public record KakaoAccount(String email) {}

  public record Properties(String nickname) {}
}
