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
import com.kusitms.kkium.resume.controller.docs.ResumeControllerDocs;
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

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/resume")
@RequiredArgsConstructor
public class ResumeController implements ResumeControllerDocs {

  private final ResumeQuestionExperienceService resumeQuestionExperienceService;
  private final ResumeWritingGuideService resumeWritingGuideService;
  private final ResumeAiDraftService resumeAiDraftService;
  private final ResumeAnswerService resumeAnswerService;
  private final JdService jdService;

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

  @PostMapping("/{jdId}")
  public ResponseEntity<ApiResponse<Void>> saveAnswers(
      @PathVariable Long jdId,
      @Valid @RequestBody ResumeAnswerSaveRequest request,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    resumeAnswerService.saveAnswers(jdId, userDetails.getId(), request);
    return ResponseEntity.ok(ApiResponse.successWithNoContent());
  }

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

  @DeleteMapping("/jd/{jdId}/questions/{questionId}")
  public ResponseEntity<ApiResponse<Void>> deleteQuestion(
      @PathVariable Long jdId,
      @PathVariable Long questionId,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    jdService.deleteQuestion(jdId, questionId, userDetails.getId());
    return ResponseEntity.ok(ApiResponse.successWithNoContent());
  }
}
