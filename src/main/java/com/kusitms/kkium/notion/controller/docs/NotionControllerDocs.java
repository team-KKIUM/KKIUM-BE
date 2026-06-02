package com.kusitms.kkium.notion.controller.docs;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import com.kusitms.kkium.global.response.ApiResponse;
import com.kusitms.kkium.notion.dto.response.NotionPageListResponse;
import com.kusitms.kkium.user.utils.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Notion", description = "Notion 연동 API")
public interface NotionControllerDocs {

  @Operation(summary = "Notion OAuth 인증 URL 반환", description = "Notion 연결을 위한 OAuth 인증 URL을 반환합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Notion OAuth 인증 URL 반환 성공",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 필요",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  ResponseEntity<ApiResponse<String>> getAuthorizationUrl(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestHeader(value = "Origin", defaultValue = "https://kkium.com") String origin);

  @Operation(
      summary = "Notion OAuth 콜백 처리",
      description = "Notion이 전달한 code로 access_token을 저장하고 프론트로 redirect합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "302",
        description = "프론트 콜백 화면으로 리다이렉트"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "유효하지 않은 state",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "Notion 토큰 발급 실패",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  ResponseEntity<Void> callback(
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String error,
      @RequestParam String state);

  @Operation(summary = "Notion 페이지 목록 조회", description = "연결된 Notion 워크스페이스의 접근 가능한 페이지 목록을 반환합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Notion 페이지 목록 조회 성공",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 또는 Notion 연결 필요",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  ResponseEntity<ApiResponse<NotionPageListResponse>> getPages(
      @AuthenticationPrincipal CustomUserDetails userDetails);
}
