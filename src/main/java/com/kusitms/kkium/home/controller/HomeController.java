package com.kusitms.kkium.home.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kusitms.kkium.global.response.ApiResponse;
import com.kusitms.kkium.home.controller.docs.HomeControllerDocs;
import com.kusitms.kkium.home.dto.response.HomeResponse;
import com.kusitms.kkium.home.service.HomeService;
import com.kusitms.kkium.user.utils.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/home")
public class HomeController implements HomeControllerDocs {

  private final HomeService homeService;

  @GetMapping
  public ResponseEntity<ApiResponse<HomeResponse>> getHome(
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    return ResponseEntity.ok(ApiResponse.success(homeService.getHome(userDetails.getId())));
  }
}
