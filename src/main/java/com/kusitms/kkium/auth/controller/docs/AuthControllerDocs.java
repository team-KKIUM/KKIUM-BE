package com.kusitms.kkium.auth.controller.docs;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.kusitms.kkium.auth.domain.type.RedirectType;
import com.kusitms.kkium.auth.dto.request.BasicLoginRequest;
import com.kusitms.kkium.auth.dto.request.BasicSignupRequest;
import com.kusitms.kkium.auth.dto.response.LoginResponse;
import com.kusitms.kkium.global.response.ApiResponse;
import com.kusitms.kkium.user.domain.type.LoginType;
import com.kusitms.kkium.user.dto.request.TermsAgreementRequest;
import com.kusitms.kkium.user.utils.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Auth", description = "인증 및 회원 관련 API")
public interface AuthControllerDocs {

  @Operation(summary = "기본 회원가입", description = "(관리자) 이름, 이메일, 비밀번호로 회원가입합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "회원가입 성공",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "요청값 검증 실패",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "이미 존재하는 이메일",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  ResponseEntity<ApiResponse<Void>> signUp(@Valid @RequestBody BasicSignupRequest request);

  @Operation(summary = "기본 로그인", description = "(관리자) 이메일과 비밀번호로 로그인합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "로그인 성공",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "요청값 검증 실패",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "이메일 또는 비밀번호 불일치",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody BasicLoginRequest request);

  @Operation(
      summary = "소셜 로그인",
      description =
          "KAKAO 또는 GOOGLE 인가 코드로 로그인합니다. 최초 로그인 시 자동 가입됩니다. redirectType으로 LOCAL/PROD redirect URI를 선택할 수 있습니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "소셜 로그인 성공",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "잘못된 로그인 타입 또는 요청값",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "소셜 토큰 발급 또는 사용자 정보 조회 실패",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  ResponseEntity<ApiResponse<LoginResponse>> socialLogin(
      @PathVariable LoginType loginType,
      @RequestParam String code,
      @RequestParam(defaultValue = "PROD") RedirectType redirectType);

  @Operation(summary = "약관 동의", description = "로그인한 사용자의 약관 동의를 완료 처리합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "약관 동의 처리 성공",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 필요",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "유저 없음",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  ResponseEntity<ApiResponse<Void>> agreeTerms(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody TermsAgreementRequest request);
}
