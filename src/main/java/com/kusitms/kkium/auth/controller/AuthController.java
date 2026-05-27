package com.kusitms.kkium.auth.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kusitms.kkium.auth.dto.request.BasicLoginRequest;
import com.kusitms.kkium.auth.dto.request.BasicSignupRequest;
import com.kusitms.kkium.auth.dto.response.LoginResponse;
import com.kusitms.kkium.auth.service.AuthService;
import com.kusitms.kkium.global.response.ApiResponse;
import com.kusitms.kkium.user.domain.type.LoginType;
import com.kusitms.kkium.user.dto.request.TermsAgreementRequest;
import com.kusitms.kkium.user.service.UserService;
import com.kusitms.kkium.user.utils.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@Tag(name = "Auth", description = "인증 및 회원 관련 API")
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final UserService userService;

  @Operation(summary = "기본 회원가입", description = "(관리자) 이름, 이메일, 비밀번호로 회원가입합니다.")
  @PostMapping("/signup")
  public ResponseEntity<ApiResponse<Void>> signUp(@Valid @RequestBody BasicSignupRequest request) {
    authService.signup(request);
    return ResponseEntity.ok(ApiResponse.successWithNoContent());
  }

  @Operation(summary = "기본 로그인", description = "(관리자) 이메일과 비밀번호로 로그인합니다.")
  @PostMapping("/login")
  public ResponseEntity<ApiResponse<LoginResponse>> login(
      @Valid @RequestBody BasicLoginRequest request) {
    return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
  }

  @Operation(summary = "소셜 로그인", description = "KAKAO 또는 GOOGLE 인가 코드로 로그인합니다. 최초 로그인 시 자동 가입됩니다.")
  @PostMapping("/login/{loginType}")
  public ResponseEntity<ApiResponse<LoginResponse>> socialLogin(
      @PathVariable LoginType loginType, @RequestParam String code) {
    return ResponseEntity.ok(ApiResponse.success(authService.socialLogin(loginType, code)));
  }

  @Operation(summary = "약관 동의", description = "로그인한 사용자의 약관 동의를 완료 처리합니다.")
  @PostMapping("/terms")
  public ResponseEntity<ApiResponse<Void>> agreeTerms(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody TermsAgreementRequest request) {
    userService.agreeTerms(userDetails.getId());
    return ResponseEntity.ok(ApiResponse.successWithNoContent());
  }
}
