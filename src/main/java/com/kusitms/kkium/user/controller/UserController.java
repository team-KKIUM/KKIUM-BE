package com.kusitms.kkium.user.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.kusitms.kkium.global.response.ApiResponse;
import com.kusitms.kkium.user.dto.request.UserCreateRequest;
import com.kusitms.kkium.user.dto.response.UserResponse;
import com.kusitms.kkium.user.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@Tag(name = "User", description = "유저 관련 API")
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @Operation(summary = "전체 유저 조회 API (TEST)")
  @GetMapping("/test")
  public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
    return ResponseEntity.ok(ApiResponse.success(userService.getAllUsers()));
  }

  @Operation(summary = "유저 단건 조회 API (TEST)")
  @GetMapping("/test/{id}")
  public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.success(userService.getUser(id)));
  }

  @Operation(summary = "유저 생성 API (TEST)")
  @PostMapping("/test")
  public ResponseEntity<ApiResponse<UserResponse>> createUser(
      @RequestBody @Valid UserCreateRequest request) {
    return ResponseEntity.ok(ApiResponse.success(userService.createUser(request)));
  }
}
