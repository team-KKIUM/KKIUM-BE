package com.kusitms.kkium.jd.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kusitms.kkium.global.response.ApiResponse;
import com.kusitms.kkium.jd.dto.response.JdListPageResponse;
import com.kusitms.kkium.jd.service.JdService;
import com.kusitms.kkium.user.utils.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "지원 관리", description = "지원 관리 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/jd")
public class JdController {

  private final JdService jdService;

  @Operation(summary = "지원 목록 조회", description = "로그인한 사용자의 지원 공고 목록을 페이지네이션으로 조회합니다.")
  @GetMapping
  public ApiResponse<JdListPageResponse> getJdList(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    return ApiResponse.success(jdService.getJdList(userDetails, page, size));
  }
}
