package com.kusitms.kkium.user.controller.docs;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

import com.kusitms.kkium.global.response.ApiResponse;
import com.kusitms.kkium.user.dto.request.UpdateProfileColorRequest;
import com.kusitms.kkium.user.dto.response.UserProfileResponse;
import com.kusitms.kkium.user.utils.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "User", description = "유저 프로필 관련 API")
public interface UserControllerDocs {

  @Operation(summary = "계정 삭제", description = "로그인한 사용자의 계정을 소프트 삭제합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "계정 삭제 성공",
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
  ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal CustomUserDetails userDetails);

  @Operation(summary = "회원 프로필 조회", description = "로그인한 사용자의 이름과 이메일을 조회합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "회원 프로필 조회 성공",
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
  ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
      @AuthenticationPrincipal CustomUserDetails userDetails);

  @Operation(
      summary = "프로필 일러스트 변경",
      description =
          """
          사용자의 프로필 일러스트를 변경합니다.
          - 0: 민트 (커피)
          - 1: 레드 (펜촉)
          - 2: 옐로우 (전구)
          - 3: 민트 (계산기)
          - 4: 블루 (노트북)
          """)
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "프로필 일러스트 변경 성공",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "잘못된 일러스트 ID",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 필요",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  ResponseEntity<ApiResponse<Void>> updateProfileColor(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody UpdateProfileColorRequest request);
}
