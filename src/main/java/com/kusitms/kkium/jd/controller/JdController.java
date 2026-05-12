package com.kusitms.kkium.jd.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kusitms.kkium.global.response.ApiResponse;
import com.kusitms.kkium.jd.dto.request.JdCreateRequest;
import com.kusitms.kkium.jd.dto.request.JdUpdateRequest;
import com.kusitms.kkium.jd.dto.response.JdFetchResponse;
import com.kusitms.kkium.jd.dto.response.JdResponse;
import com.kusitms.kkium.jd.service.JdScrapService;
import com.kusitms.kkium.jd.service.JdService;
import com.kusitms.kkium.user.utils.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@Tag(name = "JD", description = "채용공고 관련 API")
@RequestMapping("/api/v1/jd")
@RequiredArgsConstructor
public class JdController {

  private final JdService jdService;
  private final JdScrapService jdScrapService;

  @Operation(summary = "[공고등록] 채용공고 URL 파싱", description = "링크를 입력하면 공고 내용을 파싱해 반환합니다.")
  @PostMapping
  public ResponseEntity<ApiResponse<JdFetchResponse>> fetchJd(
      @Valid @RequestBody JdCreateRequest request) {
    return ResponseEntity.ok(ApiResponse.success(jdScrapService.fetchJd(request)));
  }

  @Operation(
      summary = "[지원관리(사이드시트)][자소서작성] 제목/상세내용/자소서문항/답변 불러오기 API",
      description = "JD 정보와 문항별 답변 및 AI 초안을 반환합니다.")
  @GetMapping("/{jdId}")
  public ResponseEntity<ApiResponse<JdResponse>> getJd(
      @PathVariable Long jdId, @AuthenticationPrincipal CustomUserDetails userDetails) {
    return ResponseEntity.ok(ApiResponse.success(jdService.getJd(jdId, userDetails.getId())));
  }

  @Operation(
      summary = "[지원관리(사이드시트)] 제목/상세내용/자소서문항/답변 수정 API",
      description = "사이드 시트에서 JD 정보와 문항별 답변을 수정합니다.")
  @PatchMapping("/{jdId}")
  public ResponseEntity<ApiResponse<Void>> updateJd(
      @PathVariable Long jdId,
      @RequestBody JdUpdateRequest request,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    jdService.updateJd(jdId, userDetails.getId(), request);
    return ResponseEntity.ok(ApiResponse.successWithNoContent());
  }
}
