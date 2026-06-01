package com.kusitms.kkium.resume.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kusitms.kkium.global.response.ApiResponse;
import com.kusitms.kkium.jd.service.JdService;
import com.kusitms.kkium.resume.dto.request.AiDraftRequest;
import com.kusitms.kkium.resume.dto.request.ResumeAnswerSaveRequest;
import com.kusitms.kkium.resume.dto.response.AiDraftResponse;
import com.kusitms.kkium.resume.dto.response.ResumeQuestionExperienceResponse;
import com.kusitms.kkium.resume.dto.response.ResumeWritingGuideResponse;
import com.kusitms.kkium.resume.service.ResumeAiDraftService;
import com.kusitms.kkium.resume.service.ResumeAnswerService;
import com.kusitms.kkium.resume.service.ResumeQuestionExperienceService;
import com.kusitms.kkium.resume.service.ResumeWritingGuideService;
import com.kusitms.kkium.user.utils.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Resume", description = "자소서 작성 관련 API")
@RestController
@RequestMapping("/api/v1/resume")
@RequiredArgsConstructor
public class ResumeController {

  private final ResumeQuestionExperienceService resumeQuestionExperienceService;
  private final ResumeWritingGuideService resumeWritingGuideService;
  private final ResumeAiDraftService resumeAiDraftService;
  private final ResumeAnswerService resumeAnswerService;
  private final JdService jdService;

  @Operation(
      summary = "[자소서 작성] 문항별 경험 목록 & 활용 적합도 조회",
      description = "경험 선택 모달 진입 시 호출. 해당 문항 기준으로 유저의 전체 경험에 대한 활용 적합도를 계산해 내림차순으로 반환합니다.")
  @GetMapping("/jd/{jdId}/questions/{questionId}/experiences")
  public ResponseEntity<ApiResponse<ResumeQuestionExperienceResponse>> getQuestionExperiences(
      @PathVariable Long jdId,
      @PathVariable Long questionId,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    return ResponseEntity.ok(
        ApiResponse.success(
            resumeQuestionExperienceService.getExperiencesWithFitScore(
                jdId, questionId, userDetails.getId())));
  }

  @Operation(
      summary = "[자소서 작성] 작성 가이드 생성",
      description = "선택한 경험(1~3개) 기반으로 핵심 키워드, 공고와의 연결점, 작성 가이드를 생성합니다. 경험 X 제거 시마다 재호출합니다.")
  @GetMapping("/jd/{jdId}/questions/{questionId}/writing-guide")
  public ResponseEntity<ApiResponse<ResumeWritingGuideResponse>> getWritingGuide(
      @PathVariable Long jdId,
      @PathVariable Long questionId,
      @RequestParam List<Long> experienceIds,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    return ResponseEntity.ok(
        ApiResponse.success(
            resumeWritingGuideService.generateGuide(
                jdId, questionId, experienceIds, userDetails.getId())));
  }

  @Operation(
      summary = "[자소서 작성] 자소서 저장",
      description = "문항별 초안 텍스트와 선택 경험을 저장합니다. 기존 저장 데이터가 있으면 덮어씁니다.")
  @PostMapping("/{jdId}")
  public ResponseEntity<ApiResponse<Void>> saveAnswers(
      @PathVariable Long jdId,
      @Valid @RequestBody ResumeAnswerSaveRequest request,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    resumeAnswerService.saveAnswers(jdId, userDetails.getId(), request);
    return ResponseEntity.ok(ApiResponse.successWithNoContent());
  }

  @Operation(
      summary = "[자소서 작성] AI 초안 생성",
      description = "선택한 경험(1~3개) 기반으로 자소서 문항에 대한 완성된 초안을 생성하고 저장합니다.")
  @PostMapping("/jd/{jdId}/questions/{questionId}/ai-draft")
  public ResponseEntity<ApiResponse<AiDraftResponse>> generateAiDraft(
      @PathVariable Long jdId,
      @PathVariable Long questionId,
      @Valid @RequestBody AiDraftRequest request,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    return ResponseEntity.ok(
        ApiResponse.success(
            resumeAiDraftService.generateAiDraft(
                jdId, questionId, request.experienceIds(), userDetails.getId())));
  }

  @Operation(
      summary = "[자소서 작성] 문항 삭제",
      description = "자기소개서 문항과 연결된 모든 답변(AI 초안 포함), 답변-경험 매핑을 삭제하고 이후 문항의 번호를 재정렬합니다.")
  @DeleteMapping("/jd/{jdId}/questions/{questionId}")
  public ResponseEntity<ApiResponse<Void>> deleteQuestion(
      @PathVariable Long jdId,
      @PathVariable Long questionId,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    jdService.deleteQuestion(jdId, questionId, userDetails.getId());
    return ResponseEntity.ok(ApiResponse.successWithNoContent());
  }
}
