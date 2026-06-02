package com.kusitms.kkium.notion.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestHeader;

import com.kusitms.kkium.experience.service.analyze.NotionAnalyzeService;
import com.kusitms.kkium.global.response.ApiResponse;
import com.kusitms.kkium.notion.controller.docs.NotionControllerDocs;
import com.kusitms.kkium.notion.dto.response.NotionPageListResponse;
import com.kusitms.kkium.notion.service.NotionService;
import com.kusitms.kkium.user.utils.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class NotionController implements NotionControllerDocs {

  private final NotionService notionService;
  private final NotionAnalyzeService notionAnalyzeService;

  @GetMapping("/api/v1/experiences/notion/auth")
  public ResponseEntity<ApiResponse<String>> getAuthorizationUrl(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestHeader(value = "Origin", defaultValue = "https://kkium.com") String origin) {
    String authUrl = notionService.getAuthorizationUrl(userDetails.getId(), origin);
    return ResponseEntity.ok(ApiResponse.success(authUrl));
  }

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

  @GetMapping("/api/v1/experiences/notion/pages")
  public ResponseEntity<ApiResponse<NotionPageListResponse>> getPages(
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    NotionPageListResponse response = notionAnalyzeService.getPages(userDetails.getId());
    return ResponseEntity.ok(ApiResponse.success(response));
  }
}
