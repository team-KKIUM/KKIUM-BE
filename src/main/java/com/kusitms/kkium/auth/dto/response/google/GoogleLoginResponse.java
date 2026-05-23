package com.kusitms.kkium.auth.dto.response.google;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleLoginResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("expires_in") int expiresIn,
    @JsonProperty("scope") String scope,
    @JsonProperty("token_type") String tokenType,
    @JsonProperty("id_token") String idToken) {}
