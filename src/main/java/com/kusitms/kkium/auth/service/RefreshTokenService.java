package com.kusitms.kkium.auth.service;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.INVALID_TOKEN;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.USER_NOT_FOUND;

import java.time.Duration;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import com.kusitms.kkium.auth.dto.response.AccessTokenResponse;
import com.kusitms.kkium.auth.utils.JwtTokenProvider;
import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

  private static final String REFRESH_TOKEN_KEY_PREFIX = "auth:refresh:";

  private final JwtTokenProvider jwtTokenProvider;
  private final StringRedisTemplate redisTemplate;
  private final UserRepository userRepository;

  @Value("${auth.refresh-token.expiration}")
  private long refreshTokenExpiration;

  @Value("${auth.refresh-token.cookie-name}")
  private String refreshTokenCookieName;

  @Value("${auth.refresh-token.cookie-secure}")
  private boolean refreshTokenCookieSecure;

  @Value("${auth.refresh-token.cookie-same-site}")
  private String refreshTokenCookieSameSite;

  public void issueRefreshToken(String accessToken, HttpServletResponse response) {
    String userId = jwtTokenProvider.getUserPk(accessToken);
    String refreshToken = jwtTokenProvider.createRefreshToken(userId);

    redisTemplate
        .opsForValue()
        .set(redisKey(userId), refreshToken, Duration.ofMillis(refreshTokenExpiration));

    response.addHeader(HttpHeaders.SET_COOKIE, createCookie(refreshToken).toString());
  }

  public AccessTokenResponse reissue(HttpServletRequest request) {
    String refreshToken = resolveRefreshToken(request);
    if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
      throw new BaseException(INVALID_TOKEN);
    }

    String userId = jwtTokenProvider.getUserPk(refreshToken);
    userRepository
        .findByIdAndDeleteAtIsNull(Long.parseLong(userId))
        .orElseThrow(() -> new BaseException(USER_NOT_FOUND));

    String savedRefreshToken = redisTemplate.opsForValue().get(redisKey(userId));
    if (!refreshToken.equals(savedRefreshToken)) {
      throw new BaseException(INVALID_TOKEN);
    }

    return new AccessTokenResponse(jwtTokenProvider.createAccessToken(userId));
  }

  public void logout(HttpServletRequest request, HttpServletResponse response) {
    String refreshToken = resolveRefreshTokenOrNull(request);
    if (refreshToken != null && jwtTokenProvider.validateRefreshToken(refreshToken)) {
      String userId = jwtTokenProvider.getUserPk(refreshToken);
      redisTemplate.delete(redisKey(userId));
    }

    response.addHeader(HttpHeaders.SET_COOKIE, expireCookie().toString());
  }

  private String resolveRefreshToken(HttpServletRequest request) {
    String refreshToken = resolveRefreshTokenOrNull(request);
    if (refreshToken == null || refreshToken.isBlank()) {
      throw new BaseException(INVALID_TOKEN);
    }
    return refreshToken;
  }

  private String resolveRefreshTokenOrNull(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return null;
    }

    for (Cookie cookie : cookies) {
      if (refreshTokenCookieName.equals(cookie.getName())) {
        return cookie.getValue();
      }
    }
    return null;
  }

  private ResponseCookie createCookie(String refreshToken) {
    return ResponseCookie.from(refreshTokenCookieName, refreshToken)
        .httpOnly(true)
        .secure(refreshTokenCookieSecure)
        .sameSite(refreshTokenCookieSameSite)
        .path("/api/v1/auth")
        .maxAge(Duration.ofMillis(refreshTokenExpiration))
        .build();
  }

  private ResponseCookie expireCookie() {
    return ResponseCookie.from(refreshTokenCookieName, "")
        .httpOnly(true)
        .secure(refreshTokenCookieSecure)
        .sameSite(refreshTokenCookieSameSite)
        .path("/api/v1/auth")
        .maxAge(0)
        .build();
  }

  private String redisKey(String userId) {
    return REFRESH_TOKEN_KEY_PREFIX + userId;
  }
}
