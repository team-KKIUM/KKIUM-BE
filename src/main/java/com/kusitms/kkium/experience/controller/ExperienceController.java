package com.kusitms.kkium.experience.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.kusitms.kkium.experience.domain.type.PieceType;
import com.kusitms.kkium.experience.dto.request.ExperienceCreateRequest;
import com.kusitms.kkium.experience.dto.response.ExperienceAnalyzeResponse;
import com.kusitms.kkium.experience.dto.response.ExperienceDetailResponse;
import com.kusitms.kkium.experience.dto.response.ExperienceListResponse;
import com.kusitms.kkium.experience.service.ExperienceService;
import com.kusitms.kkium.experience.service.analyze.ExperienceAnalyzeService;
import com.kusitms.kkium.experience.service.analyze.NotionAnalyzeService;
import com.kusitms.kkium.experience.service.analyze.PdfAnalyzeService;
import com.kusitms.kkium.global.response.ApiResponse;
import com.kusitms.kkium.user.utils.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@Tag(name = "Experience", description = "경험 관련 API")
@RequestMapping("/api/v1/experiences")
@RequiredArgsConstructor
@Validated
public class ExperienceController {

  private final PdfAnalyzeService pdfAnalyzeService;
  private final NotionAnalyzeService notionAnalyzeService;
  private final ExperienceAnalyzeService experienceAnalyzeService;
  private final ExperienceService experienceService;

  @Operation(
      summary = "경험 목록 조회",
      description =
          "커서 기반 페이지네이션으로 경험 목록을 조회합니다. "
              + "keyword가 있으면 경험 제목·기술태그·역량태그를 통합 검색합니다. "
              + "type 없으면 전체 조회.")
  @GetMapping
  public ResponseEntity<ApiResponse<ExperienceListResponse>> getList(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestParam(required = false) PieceType type,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Long cursor,
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
    ExperienceListResponse response =
        experienceService.getList(userDetails.getId(), type, cursor, size, keyword);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @Operation(summary = "경험 상세 조회", description = "경험 ID로 상세 정보를 조회합니다.")
  @GetMapping("/{experienceId}")
  public ResponseEntity<ApiResponse<ExperienceDetailResponse>> getDetail(
      @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long experienceId) {
    ExperienceDetailResponse response =
        experienceService.getDetail(userDetails.getId(), experienceId);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

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
      @AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam String pageId) {
    ExperienceAnalyzeResponse response = notionAnalyzeService.analyze(userDetails.getId(), pageId);
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

  @Operation(
      summary = "경험 저장",
      description =
          "type에 따라 필요한 필드가 다릅니다. ACTIVITY: name/teamNum/role/contributionRate, CAREER: company/employmentStatus, EDUCATION: organizationName/name, ETC: 추가 필드 없음")
  @PostMapping
  public ResponseEntity<ApiResponse<Void>> save(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody ExperienceCreateRequest request) {
    experienceService.save(userDetails.getId(), request);
    return ResponseEntity.ok(ApiResponse.successWithNoContent());
  }
}
