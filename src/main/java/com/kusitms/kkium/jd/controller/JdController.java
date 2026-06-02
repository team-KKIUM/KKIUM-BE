package com.kusitms.kkium.jd.controller;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.kusitms.kkium.global.response.ApiResponse;
import com.kusitms.kkium.jd.controller.docs.JdControllerDocs;
import com.kusitms.kkium.jd.dto.request.JdCreateRequest;
import com.kusitms.kkium.jd.dto.request.JdOrderUpdateRequest;
import com.kusitms.kkium.jd.dto.request.JdQuestionCreateRequest;
import com.kusitms.kkium.jd.dto.request.JdSaveRequest;
import com.kusitms.kkium.jd.dto.request.JdTitleUpdateRequest;
import com.kusitms.kkium.jd.dto.request.JdUpdateRequest;
import com.kusitms.kkium.jd.dto.response.JdAnalysisResponse;
import com.kusitms.kkium.jd.dto.response.JdExperienceAnalysisResponse;
import com.kusitms.kkium.jd.dto.response.JdFetchResponse;
import com.kusitms.kkium.jd.dto.response.JdListPageResponse;
import com.kusitms.kkium.jd.dto.response.JdMatchAnalysisResponse;
import com.kusitms.kkium.jd.dto.response.JdOcrResponse;
import com.kusitms.kkium.jd.dto.response.JdResponse;
import com.kusitms.kkium.jd.dto.response.JdSaveResponse;
import com.kusitms.kkium.jd.service.JdEmbeddingService;
import com.kusitms.kkium.jd.service.JdExperienceAnalysisService;
import com.kusitms.kkium.jd.service.JdMatchService;
import com.kusitms.kkium.jd.service.JdOcrService;
import com.kusitms.kkium.jd.service.JdScrapService;
import com.kusitms.kkium.jd.service.JdService;
import com.kusitms.kkium.user.utils.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/jd")
@RequiredArgsConstructor
public class JdController implements JdControllerDocs {

  private final JdService jdService;
  private final JdScrapService jdScrapService;
  private final JdEmbeddingService jdAnalysisService;
  private final JdMatchService jdMatchService;
  private final JdExperienceAnalysisService jdExperienceAnalysisService;
  private final JdOcrService jdOcrService;

  @PostMapping(value = "/ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<JdOcrResponse>> extractTextFromImage(
      @RequestPart MultipartFile image) {
    return ResponseEntity.ok(ApiResponse.success(jdOcrService.extractText(image)));
  }

  @PostMapping("/url")
  public ResponseEntity<ApiResponse<JdFetchResponse>> fetchJd(
      @Valid @RequestBody JdCreateRequest request) {
    return ResponseEntity.ok(ApiResponse.success(jdScrapService.fetchJd(request)));
  }

  @PostMapping("/ai")
  public ResponseEntity<ApiResponse<JdSaveResponse>> saveJd(
      @Valid @RequestBody JdSaveRequest request,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    JdSaveResponse response = jdService.saveJd(userDetails.getId(), request);
    jdAnalysisService.analyzeAndUpdate(response.jdId(), request.content());
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @GetMapping("/{jdId}")
  public ResponseEntity<ApiResponse<JdAnalysisResponse>> getJdAnalysis(@PathVariable Long jdId) {
    return ResponseEntity.ok(ApiResponse.success(jdService.getJdAnalysis(jdId)));
  }

  @GetMapping("/{jdId}/resume")
  public ResponseEntity<ApiResponse<JdResponse>> getJd(
      @PathVariable Long jdId, @AuthenticationPrincipal CustomUserDetails userDetails) {
    return ResponseEntity.ok(ApiResponse.success(jdService.getJd(jdId, userDetails.getId())));
  }

  @PostMapping("/{jdId}/resume/questions")
  public ResponseEntity<ApiResponse<Void>> addQuestion(
      @PathVariable Long jdId,
      @Valid @RequestBody JdQuestionCreateRequest request,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    jdService.addQuestion(jdId, userDetails.getId(), request);
    return ResponseEntity.ok(ApiResponse.successWithNoContent());
  }

  @PatchMapping("/{jdId}/resume")
  public ResponseEntity<ApiResponse<Void>> updateJd(
      @PathVariable Long jdId,
      @Valid @RequestBody JdUpdateRequest request,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    jdService.updateJd(jdId, userDetails.getId(), request);
    return ResponseEntity.ok(ApiResponse.successWithNoContent());
  }

  @GetMapping
  public ResponseEntity<ApiResponse<JdListPageResponse>> getJdList(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String keyword) {
    return ResponseEntity.ok(
        ApiResponse.success(jdService.getJdList(userDetails, page, size, keyword)));
  }

  @PatchMapping("/{jdId}/target")
  public ResponseEntity<ApiResponse<Void>> toggleTarget(
      @PathVariable Long jdId, @AuthenticationPrincipal CustomUserDetails userDetails) {
    jdService.toggleTarget(jdId, userDetails);
    return ResponseEntity.ok(ApiResponse.successWithNoContent());
  }

  @DeleteMapping("/{jdId}")
  public ResponseEntity<ApiResponse<Void>> deleteJd(
      @PathVariable Long jdId, @AuthenticationPrincipal CustomUserDetails userDetails) {
    jdService.deleteJd(jdId, userDetails);
    return ResponseEntity.ok(ApiResponse.successWithNoContent());
  }

  @PatchMapping("/{jdId}/title")
  public ResponseEntity<ApiResponse<Void>> updateTitle(
      @PathVariable Long jdId,
      @RequestBody @Valid JdTitleUpdateRequest request,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    jdService.updateTitle(jdId, request, userDetails);
    return ResponseEntity.ok(ApiResponse.successWithNoContent());
  }

  @PatchMapping("/order")
  public ResponseEntity<ApiResponse<Void>> updateOrder(
      @RequestBody @Valid JdOrderUpdateRequest request,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    jdService.updateOrder(request, userDetails);
    return ResponseEntity.ok(ApiResponse.successWithNoContent());
  }

  @GetMapping("/{jdId}/analysis")
  public ResponseEntity<ApiResponse<JdMatchAnalysisResponse>> getMatchAnalysis(
      @PathVariable Long jdId, @AuthenticationPrincipal CustomUserDetails userDetails) {
    return ResponseEntity.ok(
        ApiResponse.success(jdMatchService.analyze(jdId, userDetails.getId())));
  }

  @GetMapping("/{jdId}/analysis/experiences/{experienceId}")
  public ResponseEntity<ApiResponse<JdExperienceAnalysisResponse>> getExperienceAnalysis(
      @PathVariable Long jdId,
      @PathVariable Long experienceId,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    return ResponseEntity.ok(
        ApiResponse.success(
            jdExperienceAnalysisService.analyze(jdId, experienceId, userDetails.getId())));
  }
}
