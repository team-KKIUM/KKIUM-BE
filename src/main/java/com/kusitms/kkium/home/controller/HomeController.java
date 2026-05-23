package com.kusitms.kkium.home.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kusitms.kkium.global.response.ApiResponse;
import com.kusitms.kkium.home.dto.response.HomeResponse;
import com.kusitms.kkium.home.service.HomeService;
import com.kusitms.kkium.user.utils.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Home", description = "홈 대시보드 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/home")
public class HomeController {

  private final HomeService homeService;

  @Operation(summary = "홈 대시보드 조회")
  @GetMapping
  public ResponseEntity<ApiResponse<HomeResponse>> getHome(
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    return ResponseEntity.ok(ApiResponse.success(homeService.getHome(userDetails.getId())));
  }
}
