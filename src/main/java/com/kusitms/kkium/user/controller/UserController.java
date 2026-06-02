package com.kusitms.kkium.user.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kusitms.kkium.global.response.ApiResponse;
import com.kusitms.kkium.user.controller.docs.UserControllerDocs;
import com.kusitms.kkium.user.dto.request.UpdateProfileColorRequest;
import com.kusitms.kkium.user.dto.response.UserProfileResponse;
import com.kusitms.kkium.user.service.UserService;
import com.kusitms.kkium.user.utils.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController implements UserControllerDocs {

  private final UserService userService;

  @DeleteMapping("/me/delete")
  public ResponseEntity<ApiResponse<Void>> delete(
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    userService.delete(userDetails.getId());
    return ResponseEntity.ok(ApiResponse.successWithNoContent());
  }

  @GetMapping("/me/profile")
  public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    return ResponseEntity.ok(ApiResponse.success(userService.getProfile(userDetails.getId())));
  }

  @PatchMapping("/me/profile-color")
  public ResponseEntity<ApiResponse<Void>> updateProfileColor(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody UpdateProfileColorRequest request) {
    userService.updateProfileColor(userDetails.getId(), request.illustrateId());
    return ResponseEntity.ok(ApiResponse.successWithNoContent());
  }
}
