package com.kusitms.kkium.home.controller.docs;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.kusitms.kkium.global.response.ApiResponse;
import com.kusitms.kkium.home.dto.response.HomeResponse;
import com.kusitms.kkium.user.utils.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Home", description = "홈 대시보드 API")
public interface HomeControllerDocs {

  @Operation(summary = "홈 대시보드 조회")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "홈 대시보드 조회 성공",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 필요",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  ResponseEntity<ApiResponse<HomeResponse>> getHome(
      @AuthenticationPrincipal CustomUserDetails userDetails);
}
