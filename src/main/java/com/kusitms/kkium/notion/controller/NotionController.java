package com.kusitms.kkium.notion.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.kusitms.kkium.experience.service.analyze.NotionAnalyzeService;
import com.kusitms.kkium.global.response.ApiResponse;
import com.kusitms.kkium.notion.dto.response.NotionPageListResponse;
import com.kusitms.kkium.notion.service.NotionService;
import com.kusitms.kkium.user.utils.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@Tag(name = "Notion", description = "Notion 연동 API")
@RequiredArgsConstructor
public class NotionController {

  private final NotionService notionService;
  private final NotionAnalyzeService notionAnalyzeService;

  @Operation(summary = "Notion OAuth 인증 URL 반환", description = "Notion 연결을 위한 OAuth 인증 URL을 반환합니다.")
  @GetMapping("/api/v1/experiences/notion/auth")
  public ResponseEntity<ApiResponse<String>> getAuthorizationUrl(
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    String authUrl = notionService.getAuthorizationUrl(userDetails.getId());
    return ResponseEntity.ok(ApiResponse.success(authUrl));
  }

  @Operation(
      summary = "Notion OAuth 콜백 처리",
      description = "Notion이 전달한 code로 access_token을 저장하고 프론트로 redirect합니다.")
  @GetMapping("/api/v1/notion/callback")
  public ResponseEntity<Void> callback(
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String error,
      @RequestParam String state) {
    if (error != null) {
      return ResponseEntity.status(302)
          .location(URI.create("https://kkium.com/experience/add?success=false"))
          .build();
    }
    String redirectUrl = notionService.handleCallback(code, state);
    return ResponseEntity.status(302).location(URI.create(redirectUrl)).build();
  }

  @Operation(summary = "Notion 페이지 목록 조회", description = "연결된 Notion 워크스페이스의 접근 가능한 페이지 목록을 반환합니다.")
  @GetMapping("/api/v1/experiences/notion/pages")
  public ResponseEntity<ApiResponse<NotionPageListResponse>> getPages(
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    NotionPageListResponse response = notionAnalyzeService.getPages(userDetails.getId());
    return ResponseEntity.ok(ApiResponse.success(response));
  }
}
