package com.kusitms.kkium.user.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kusitms.kkium.global.response.ApiResponse;
import com.kusitms.kkium.user.dto.request.UpdateProfileColorRequest;
import com.kusitms.kkium.user.dto.response.UserProfileResponse;
import com.kusitms.kkium.user.service.UserService;
import com.kusitms.kkium.user.utils.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@Tag(name = "User", description = "유저 프로필 관련 API")
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @Operation(summary = "회원 프로필 조회", description = "로그인한 사용자의 이름과 이메일을 조회합니다.")
  @GetMapping("/me/profile")
  public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    return ResponseEntity.ok(ApiResponse.success(userService.getProfile(userDetails.getId())));
  }

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
  @PatchMapping("/me/profile-color")
  public ResponseEntity<ApiResponse<Void>> updateProfileColor(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody UpdateProfileColorRequest request) {
    userService.updateProfileColor(userDetails.getId(), request.illustrateId());
    return ResponseEntity.ok(ApiResponse.successWithNoContent());
  }
}
