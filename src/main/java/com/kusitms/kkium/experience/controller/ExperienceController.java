package com.kusitms.kkium.experience.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
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

import com.kusitms.kkium.experience.controller.docs.ExperienceControllerDocs;
import com.kusitms.kkium.experience.domain.type.PieceType;
import com.kusitms.kkium.experience.dto.request.ExperienceCreateRequest;
import com.kusitms.kkium.experience.dto.request.ExperienceOrderUpdateRequest;
import com.kusitms.kkium.experience.dto.request.ExperienceTitleUpdateRequest;
import com.kusitms.kkium.experience.dto.request.ExperienceUpdateRequest;
import com.kusitms.kkium.experience.dto.response.ExperienceAnalyzeResponse;
import com.kusitms.kkium.experience.dto.response.ExperienceDetailResponse;
import com.kusitms.kkium.experience.dto.response.ExperienceListResponse;
import com.kusitms.kkium.experience.service.ExperienceService;
import com.kusitms.kkium.experience.service.analyze.ExperienceAnalyzeService;
import com.kusitms.kkium.experience.service.analyze.NotionAnalyzeService;
import com.kusitms.kkium.experience.service.analyze.PdfAnalyzeService;
import com.kusitms.kkium.global.response.ApiResponse;
import com.kusitms.kkium.user.utils.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/experiences")
@RequiredArgsConstructor
@Validated
public class ExperienceController implements ExperienceControllerDocs {

  private final PdfAnalyzeService pdfAnalyzeService;
  private final NotionAnalyzeService notionAnalyzeService;
  private final ExperienceAnalyzeService experienceAnalyzeService;
  private final ExperienceService experienceService;

  @GetMapping
  public ResponseEntity<ApiResponse<ExperienceListResponse>> getList(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestParam(required = false) PieceType type,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Integer cursor,
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
    ExperienceListResponse response =
        experienceService.getList(userDetails.getId(), type, cursor, size, keyword);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @GetMapping("/{experienceId}")
  public ResponseEntity<ApiResponse<ExperienceDetailResponse>> getDetail(
      @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long experienceId) {
    ExperienceDetailResponse response =
        experienceService.getDetail(userDetails.getId(), experienceId);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @PostMapping(value = "/analyze/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<ExperienceAnalyzeResponse>> analyzePdf(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestPart("file") MultipartFile file) {
    ExperienceAnalyzeResponse response = pdfAnalyzeService.analyzePdf(userDetails.getId(), file);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @PostMapping("/analyze/notion")
  public ResponseEntity<ApiResponse<ExperienceAnalyzeResponse>> analyzeNotion(
      @AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam String pageId) {
    ExperienceAnalyzeResponse response = notionAnalyzeService.analyze(userDetails.getId(), pageId);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<ExperienceAnalyzeResponse>> analyzeMerge(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestPart(value = "file", required = false) MultipartFile file,
      @RequestPart(value = "pageId", required = false) String pageId) {
    ExperienceAnalyzeResponse response =
        experienceAnalyzeService.analyze(userDetails.getId(), file, pageId);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<Void>> save(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody ExperienceCreateRequest request) {
    experienceService.save(userDetails.getId(), request);
    return ResponseEntity.ok(ApiResponse.successWithNoContent());
  }

  @PatchMapping("/order")
  public ResponseEntity<ApiResponse<Void>> updateOrder(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody ExperienceOrderUpdateRequest request) {
    experienceService.updateOrder(request, userDetails.getId());
    return ResponseEntity.ok(ApiResponse.successWithNoContent());
  }

  @DeleteMapping("/{experienceId}")
  public ResponseEntity<ApiResponse<Void>> delete(
      @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long experienceId) {
    experienceService.delete(userDetails.getId(), experienceId);
    return ResponseEntity.ok(ApiResponse.successWithNoContent());
  }

  @PatchMapping("/{experienceId}")
  public ResponseEntity<ApiResponse<Void>> update(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long experienceId,
      @Valid @RequestBody ExperienceUpdateRequest request) {
    experienceService.update(userDetails.getId(), experienceId, request);
    return ResponseEntity.ok(ApiResponse.successWithNoContent());
  }

  @PatchMapping("/{experienceId}/title")
  public ResponseEntity<ApiResponse<Void>> updateTitle(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long experienceId,
      @Valid @RequestBody ExperienceTitleUpdateRequest request) {
    experienceService.updateTitle(userDetails.getId(), experienceId, request.title());
    return ResponseEntity.ok(ApiResponse.successWithNoContent());
  }
}
