package com.kusitms.kkium.jd.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kusitms.kkium.global.response.ApiResponse;
import com.kusitms.kkium.jd.dto.response.JdQuestionExperienceResponse;
import com.kusitms.kkium.jd.service.JdQuestionExperienceService;
import com.kusitms.kkium.user.utils.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Resume", description = "자소서 작성 관련 API")
@RestController
@RequestMapping("/api/v1/resume")
@RequiredArgsConstructor
public class ResumeController {

  private final JdQuestionExperienceService jdQuestionExperienceService;

  @Operation(
      summary = "[자소서 작성] 문항별 경험 목록 & 활용 적합도 조회",
      description = "경험 선택 모달 진입 시 호출. 해당 문항 기준으로 유저의 전체 경험에 대한 활용 적합도를 계산해 내림차순으로 반환합니다.")
  @GetMapping("/jd/{jdId}/questions/{questionId}/experiences")
  public ResponseEntity<ApiResponse<JdQuestionExperienceResponse>> getQuestionExperiences(
      @PathVariable Long jdId,
      @PathVariable Long questionId,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    return ResponseEntity.ok(
        ApiResponse.success(
            jdQuestionExperienceService.getExperiencesWithFitScore(
                jdId, questionId, userDetails.getId())));
  }
}
