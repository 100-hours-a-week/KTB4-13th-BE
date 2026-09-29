package com.book.core.support.api.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Support", description = "서버 상태 확인 API")
public interface SupportControllerSpec {
    @Operation(summary = "서버 상태 확인", description = "서버가 HTTP 요청을 처리할 수 있는지 확인합니다.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "서버가 요청을 정상적으로 처리할 수 있음")})
    ResponseEntity<com.book.common.response.ApiResponse<Void>> checkHealth();
}
