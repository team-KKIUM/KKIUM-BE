package com.kusitms.kkium.jd.controller;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kusitms.kkium.global.response.ApiResponse;
import com.kusitms.kkium.jd.dto.request.JdTitleUpdateRequest;
import com.kusitms.kkium.jd.dto.response.JdListPageResponse;
import com.kusitms.kkium.jd.service.JdService;
import com.kusitms.kkium.user.utils.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "JD", description = "지원 관리 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/jd")
public class JdController {

  private final JdService jdService;

  @Operation(
      summary = "[지원 관리] 공고 전체 목록 조회",
      description = "로그인한 사용자의 지원 관리에서 공고 목록을 페이지네이션으로 조회합니다.")
  @GetMapping
  public ApiResponse<JdListPageResponse> getJdList(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    return ApiResponse.success(jdService.getJdList(userDetails, page, size));
  }

  @Operation(
      summary = "[지원 관리] 목표 공고 설정/해제",
      description = "지원 공고의 목표 공고 여부를 토글합니다. 최대 5개까지 설정 가능합니다.")
  @PatchMapping("/{jdId}/target")
  public ApiResponse<Void> toggleTarget(
      @PathVariable Long jdId, @AuthenticationPrincipal CustomUserDetails userDetails) {
    jdService.toggleTarget(jdId, userDetails);
    return ApiResponse.success(null);
  }

  @Operation(summary = "[지원 관리] 공고 단건 삭제", description = "지원 관리에서 공고를 소프트 삭제합니다.")
  @DeleteMapping("/{jdId}")
  public ApiResponse<Void> deleteJd(
      @PathVariable Long jdId, @AuthenticationPrincipal CustomUserDetails userDetails) {

    jdService.deleteJd(jdId, userDetails);
    return ApiResponse.success(null);
  }

  @Operation(summary = "[지원 관리] 공고 단건 제목 수정", description = "지원 관리에서 공고 1개의 제목을 수정합니다.")
  @PatchMapping("/{jdId}/title")
  public ApiResponse<Void> updateTitle(
      @PathVariable Long jdId,
      @RequestBody @Valid JdTitleUpdateRequest request,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    jdService.updateTitle(jdId, request, userDetails);
    return ApiResponse.success(null);
  }
}
