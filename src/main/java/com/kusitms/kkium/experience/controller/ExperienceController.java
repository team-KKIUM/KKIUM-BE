package com.kusitms.kkium.experience.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.kusitms.kkium.experience.dto.response.ExperienceAnalyzeResponse;
import com.kusitms.kkium.experience.service.ExperienceAnalyzeService;
import com.kusitms.kkium.experience.service.NotionAnalyzeService;
import com.kusitms.kkium.experience.service.PdfAnalyzeService;
import com.kusitms.kkium.global.response.ApiResponse;
import com.kusitms.kkium.user.utils.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@Tag(name = "Experience", description = "경험 관련 API")
@RequestMapping("/api/v1/experiences")
@RequiredArgsConstructor
public class ExperienceController {

  private final PdfAnalyzeService pdfAnalyzeService;
  private final NotionAnalyzeService notionAnalyzeService;
  private final ExperienceAnalyzeService experienceAnalyzeService;

  @Operation(
      summary = "PDF 자료 분석",
      description = "PDF 파일을 업로드하면 텍스트를 추출하여 AI가 경험을 분석합니다. PDF 원본은 서버에 저장되지 않습니다.")
  @PostMapping(value = "/analyze/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<ExperienceAnalyzeResponse>> analyzePdf(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestPart("file") MultipartFile file) {
    ExperienceAnalyzeResponse response = pdfAnalyzeService.analyzePdf(userDetails.getId(), file);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @Operation(
      summary = "Notion 페이지 LLM 분석",
      description = "선택한 Notion 페이지의 콘텐츠를 추출하여 AI가 경험을 분석합니다.")
  @PostMapping("/analyze/notion")
  public ResponseEntity<ApiResponse<ExperienceAnalyzeResponse>> analyzeNotion(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestParam String pageId) {
    ExperienceAnalyzeResponse response =
        notionAnalyzeService.analyze(userDetails.getId(), pageId);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @Operation(
      summary = "[테스트] PDF + Notion 통합 분석",
      description = "PDF와 Notion 페이지 내용을 합쳐서 AI가 한 번에 경험을 분석합니다. 둘 중 하나 이상 필수.")
  @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<ExperienceAnalyzeResponse>> analyzeMerge(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestPart(value = "file", required = false) MultipartFile file,
      @RequestPart(value = "pageId", required = false) String pageId) {
    ExperienceAnalyzeResponse response =
        experienceAnalyzeService.analyze(userDetails.getId(), file, pageId);
    return ResponseEntity.ok(ApiResponse.success(response));
  }
}
