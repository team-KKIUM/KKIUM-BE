package com.kusitms.kkium.global.controller.docs;

import org.springframework.http.ResponseEntity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Health", description = "서버 상태 확인 API")
public interface HealthControllerDocs {

  @Operation(summary = "헬스 체크", description = "서버가 정상적으로 동작하는지 확인합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "서버 상태 정상")
  })
  ResponseEntity<String> health();
}
