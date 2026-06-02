package com.kusitms.kkium.jd.controller.docs;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import com.kusitms.kkium.global.response.ApiResponse;
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
import com.kusitms.kkium.user.utils.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "JD", description = "지원 관리 관련 API")
public interface JdControllerDocs {

  @Operation(
      summary = "[공고등록] 채용공고 이미지 OCR",
      description = "이미지를 업로드하면 Google Cloud Vision API로 텍스트를 추출해 반환합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "채용공고 이미지 OCR 성공",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "잘못된 이미지 형식",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "500",
        description = "이미지 텍스트 추출 실패",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  ResponseEntity<ApiResponse<JdOcrResponse>> extractTextFromImage(@RequestPart MultipartFile image);

  @Operation(summary = "[공고등록] 채용공고 URL 파싱", description = "링크를 입력하면 공고 내용을 파싱해 반환합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "채용공고 URL 파싱 성공",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "요청값 검증 실패",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "502",
        description = "채용공고 내용 조회 실패",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  ResponseEntity<ApiResponse<JdFetchResponse>> fetchJd(@Valid @RequestBody JdCreateRequest request);

  @Operation(
      summary = "[공고등록] 채용공고 저장",
      description = "파싱된 공고 내용을 저장합니다. 저장 후 AI가 태그/역량/업무를 비동기로 분석합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "채용공고 저장 성공",
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
  ResponseEntity<ApiResponse<JdSaveResponse>> saveJd(
      @Valid @RequestBody JdSaveRequest request,
      @AuthenticationPrincipal CustomUserDetails userDetails);

  @Operation(
      summary = "[공고분석] AI 분석 상태 및 결과 조회",
      description = "analysisStatus가 COMPLETED가 될 때까지 폴링하여 분석 결과를 확인합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "공고 분석 결과 조회 성공",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "공고 없음",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  ResponseEntity<ApiResponse<JdAnalysisResponse>> getJdAnalysis(@PathVariable Long jdId);

  @Operation(
      summary = "[지원관리(사이드시트)][자소서작성] 제목/상세내용/자소서문항/답변 불러오기 API",
      description = "JD 정보와 문항별 답변 및 AI 초안을 반환합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "자소서 작성 화면 조회 성공",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 필요",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "공고 또는 유저 없음",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  ResponseEntity<ApiResponse<JdResponse>> getJd(
      @PathVariable Long jdId, @AuthenticationPrincipal CustomUserDetails userDetails);

  @Operation(
      summary = "[자소서작성] 자소서 문항 추가 API",
      description = "공고의 자기소개서 문항을 추가합니다. 추가된 문항은 마지막 순서로 배치됩니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "자소서 문항 추가 성공",
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
        responseCode = "403",
        description = "공고 접근 권한 없음",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "공고 없음",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  ResponseEntity<ApiResponse<Void>> addQuestion(
      @PathVariable Long jdId,
      @Valid @RequestBody JdQuestionCreateRequest request,
      @AuthenticationPrincipal CustomUserDetails userDetails);

  @Operation(
      summary = "[지원관리(사이드시트)] 제목/상세내용/자소서문항/답변 수정 API",
      description = "사이드 시트에서 JD 정보와 문항별 답변을 수정합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "공고 및 자소서 수정 성공",
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
        description = "공고, 문항 또는 유저 없음",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  ResponseEntity<ApiResponse<Void>> updateJd(
      @PathVariable Long jdId,
      @Valid @RequestBody JdUpdateRequest request,
      @AuthenticationPrincipal CustomUserDetails userDetails);

  @Operation(
      summary = "[지원 관리] 공고 전체 목록 조회",
      description = "로그인한 사용자의 지원 관리에서 공고 목록을 조회합니다. keyword 입력 시 공고명/기업명/모집분야에 포함된 단어 기준으로 검색합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "공고 목록 조회 성공",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 필요",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  ResponseEntity<ApiResponse<JdListPageResponse>> getJdList(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String keyword);

  @Operation(
      summary = "[지원 관리] 목표 공고 설정/해제",
      description = "지원 공고의 목표 공고 여부를 토글합니다. 최대 5개까지 설정 가능합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "목표 공고 설정/해제 성공",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "목표 공고 최대 개수 초과",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 필요",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "공고 접근 권한 없음",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "공고 없음",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  ResponseEntity<ApiResponse<Void>> toggleTarget(
      @PathVariable Long jdId, @AuthenticationPrincipal CustomUserDetails userDetails);

  @Operation(summary = "[지원 관리] 공고 단건 삭제", description = "지원 관리에서 공고를 소프트 삭제합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "공고 삭제 성공",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 필요",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "공고 접근 권한 없음",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "공고 없음",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  ResponseEntity<ApiResponse<Void>> deleteJd(
      @PathVariable Long jdId, @AuthenticationPrincipal CustomUserDetails userDetails);

  @Operation(summary = "[지원 관리] 공고 단건 제목 수정", description = "지원 관리에서 공고 1개의 제목을 수정합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "공고 제목 수정 성공",
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
        responseCode = "403",
        description = "공고 접근 권한 없음",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "공고 없음",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  ResponseEntity<ApiResponse<Void>> updateTitle(
      @PathVariable Long jdId,
      @RequestBody @Valid JdTitleUpdateRequest request,
      @AuthenticationPrincipal CustomUserDetails userDetails);

  @Operation(summary = "[지원 관리] 카드 그리드 순서 수정", description = "드래그 앤 드롭으로 카드 순서를 변경합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "공고 카드 순서 변경 성공",
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
        responseCode = "403",
        description = "공고 접근 권한 없음",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  ResponseEntity<ApiResponse<Void>> updateOrder(
      @RequestBody @Valid JdOrderUpdateRequest request,
      @AuthenticationPrincipal CustomUserDetails userDetails);

  @Operation(summary = "[공고분석] 공고 분석 및 경험 매칭", description = "공고 분석 결과와 경험별 활용 적합도, 지원 적합도를 반환합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "공고 분석 및 경험 매칭 조회 성공",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 필요",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "공고 없음",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  ResponseEntity<ApiResponse<JdMatchAnalysisResponse>> getMatchAnalysis(
      @PathVariable Long jdId, @AuthenticationPrincipal CustomUserDetails userDetails);

  @Operation(
      summary = "[공고분석] 경험 카드 상세 분석",
      description = "경험 카드 클릭 시 좋은 점 / 부족한 점 / 활용 가이드 / 하이라이팅 키워드를 반환합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "경험 카드 상세 분석 성공",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 필요",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "공고 또는 경험 없음",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  ResponseEntity<ApiResponse<JdExperienceAnalysisResponse>> getExperienceAnalysis(
      @PathVariable Long jdId,
      @PathVariable Long experienceId,
      @AuthenticationPrincipal CustomUserDetails userDetails);
}
