package com.kusitms.kkium.experience.controller.docs;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import com.kusitms.kkium.experience.domain.type.PieceType;
import com.kusitms.kkium.experience.dto.request.ExperienceCreateRequest;
import com.kusitms.kkium.experience.dto.request.ExperienceOrderUpdateRequest;
import com.kusitms.kkium.experience.dto.request.ExperienceTitleUpdateRequest;
import com.kusitms.kkium.experience.dto.request.ExperienceUpdateRequest;
import com.kusitms.kkium.experience.dto.response.ExperienceAnalyzeResponse;
import com.kusitms.kkium.experience.dto.response.ExperienceDetailResponse;
import com.kusitms.kkium.experience.dto.response.ExperienceListResponse;
import com.kusitms.kkium.global.response.ApiResponse;
import com.kusitms.kkium.user.utils.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Experience", description = "경험 관련 API")
public interface ExperienceControllerDocs {

  @Operation(
      summary = "경험 목록 조회",
      description =
          "커서 기반 페이지네이션으로 경험 목록을 조회합니다. "
              + "keyword가 있으면 경험 제목·기술태그·역량태그를 통합 검색합니다. "
              + "type 없으면 전체 조회.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "경험 목록 조회 성공",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "페이지 크기 검증 실패",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 필요",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  ResponseEntity<ApiResponse<ExperienceListResponse>> getList(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestParam(required = false) PieceType type,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Integer cursor,
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size);

  @Operation(summary = "경험 상세 조회", description = "경험 ID로 상세 정보를 조회합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "경험 상세 조회 성공",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 필요",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "경험 없음",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  ResponseEntity<ApiResponse<ExperienceDetailResponse>> getDetail(
      @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long experienceId);

  @Operation(
      summary = "PDF 자료 분석",
      description = "PDF 파일을 업로드하면 텍스트를 추출하여 AI가 경험을 분석합니다. PDF 원본은 서버에 저장되지 않습니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "PDF 자료 분석 성공",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "잘못된 파일 형식",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 필요",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "500",
        description = "PDF 파싱 또는 AI 분석 실패",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  ResponseEntity<ApiResponse<ExperienceAnalyzeResponse>> analyzePdf(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestPart("file") MultipartFile file);

  @Operation(
      summary = "Notion 페이지 LLM 분석",
      description = "선택한 Notion 페이지의 콘텐츠를 추출하여 AI가 경험을 분석합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Notion 페이지 분석 성공",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 또는 Notion 연결 필요",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "500",
        description = "Notion 블록 파싱 또는 AI 분석 실패",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  ResponseEntity<ApiResponse<ExperienceAnalyzeResponse>> analyzeNotion(
      @AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam String pageId);

  @Operation(
      summary = "[테스트] PDF + Notion 통합 분석",
      description = "PDF와 Notion 페이지 내용을 합쳐서 AI가 한 번에 경험을 분석합니다. 둘 중 하나 이상 필수.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "통합 분석 성공",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "요청값 또는 파일 형식 오류",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 필요",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "500",
        description = "자료 파싱 또는 AI 분석 실패",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  ResponseEntity<ApiResponse<ExperienceAnalyzeResponse>> analyzeMerge(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestPart(value = "file", required = false) MultipartFile file,
      @RequestPart(value = "pageId", required = false) String pageId);

  @Operation(
      summary = "경험 저장",
      description =
          "type에 따라 필요한 필드가 다릅니다. ACTIVITY: name/teamNum/role/contributionRate, CAREER: company/employmentStatus, EDUCATION: organizationName/name, ETC: 추가 필드 없음")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "경험 저장 성공",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "요청값 검증 실패",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 필요",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  ResponseEntity<ApiResponse<Void>> save(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody ExperienceCreateRequest request);

  @Operation(summary = "경험 카드 순서 변경", description = "드래그 앤 드롭으로 카드 순서를 변경합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "경험 카드 순서 변경 성공",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "요청값 검증 실패",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 필요",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "경험 순서 정보 없음",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  ResponseEntity<ApiResponse<Void>> updateOrder(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody ExperienceOrderUpdateRequest request);

  @Operation(summary = "경험 삭제", description = "경험을 소프트 삭제합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "경험 삭제 성공",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 필요",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "경험 없음",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  ResponseEntity<ApiResponse<Void>> delete(
      @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long experienceId);

  @Operation(
      summary = "경험 수정",
      description =
          """
          경험 상세 정보를 수정합니다.
          type에 따라 detail 필드가 다릅니다.
          - ACTIVITY: name, teamNum, role, contributionRate, startDate, endDate
          - CAREER: company, employmentStatus, startDate, endDate
          - EDUCATION: name, organizationName, startDate, endDate
          - ETC: startDate, endDate
          """)
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "경험 수정 성공",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "요청값 검증 실패",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 필요",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "경험 없음",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  ResponseEntity<ApiResponse<Void>> update(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long experienceId,
      @Valid @RequestBody ExperienceUpdateRequest request);

  @Operation(summary = "경험 제목 수정", description = "경험 카드의 제목을 수정합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "경험 제목 수정 성공",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "요청값 검증 실패",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 필요",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "경험 없음",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  ResponseEntity<ApiResponse<Void>> updateTitle(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long experienceId,
      @Valid @RequestBody ExperienceTitleUpdateRequest request);
}
