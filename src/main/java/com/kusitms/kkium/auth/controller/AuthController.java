package com.kusitms.kkium.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kusitms.kkium.auth.controller.docs.AuthControllerDocs;
import com.kusitms.kkium.auth.domain.type.RedirectType;
import com.kusitms.kkium.auth.dto.request.BasicLoginRequest;
import com.kusitms.kkium.auth.dto.request.BasicSignupRequest;
import com.kusitms.kkium.auth.dto.response.AccessTokenResponse;
import com.kusitms.kkium.auth.dto.response.LoginResponse;
import com.kusitms.kkium.auth.service.AuthService;
import com.kusitms.kkium.auth.service.RefreshTokenService;
import com.kusitms.kkium.global.response.ApiResponse;
import com.kusitms.kkium.user.domain.type.LoginType;
import com.kusitms.kkium.user.dto.request.TermsAgreementRequest;
import com.kusitms.kkium.user.service.UserService;
import com.kusitms.kkium.user.utils.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocs {

  private final AuthService authService;
  private final UserService userService;
  private final RefreshTokenService refreshTokenService;

  @PostMapping("/signup")
  public ResponseEntity<ApiResponse<Void>> signUp(@Valid @RequestBody BasicSignupRequest request) {
    authService.signup(request);
    return ResponseEntity.ok(ApiResponse.successWithNoContent());
  }

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<LoginResponse>> login(
      @Valid @RequestBody BasicLoginRequest request, HttpServletResponse response) {
    LoginResponse loginResponse = authService.login(request);
    refreshTokenService.issueRefreshToken(loginResponse.accessToken(), response);
    return ResponseEntity.ok(ApiResponse.success(loginResponse));
  }

  @PostMapping("/login/{loginType}")
  public ResponseEntity<ApiResponse<LoginResponse>> socialLogin(
      @PathVariable LoginType loginType,
      @RequestParam String code,
      @RequestParam(defaultValue = "PROD") RedirectType redirectType,
      HttpServletResponse response) {
    LoginResponse loginResponse = authService.socialLogin(loginType, code, redirectType);
    refreshTokenService.issueRefreshToken(loginResponse.accessToken(), response);
    return ResponseEntity.ok(ApiResponse.success(loginResponse));
  }

  @PostMapping("/reissue")
  public ResponseEntity<ApiResponse<AccessTokenResponse>> reissue(HttpServletRequest request) {
    return ResponseEntity.ok(ApiResponse.success(refreshTokenService.reissue(request)));
  }

  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> logout(
      HttpServletRequest request, HttpServletResponse response) {
    refreshTokenService.logout(request, response);
    return ResponseEntity.ok(ApiResponse.successWithNoContent());
  }

  @PostMapping("/terms")
  public ResponseEntity<ApiResponse<Void>> agreeTerms(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody TermsAgreementRequest request) {
    userService.agreeTerms(userDetails.getId());
    return ResponseEntity.ok(ApiResponse.successWithNoContent());
  }
}
