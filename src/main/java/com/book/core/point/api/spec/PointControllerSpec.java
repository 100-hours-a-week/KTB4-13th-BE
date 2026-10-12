package com.book.core.point.api.spec;

import com.book.core.point.api.response.PointResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

@Tag(name = "Point", description = "포인트 API")
public interface PointControllerSpec {
    @Operation(summary = "포인트 조회", description = "인증된 회원의 포인트 잔액과 포인트 이력을 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "포인트 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않음"),
        @ApiResponse(responseCode = "404", description = "포인트 잔액 정보를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "알 수 없는 오류")})
    @Parameter(in = ParameterIn.HEADER, name = HttpHeaders.AUTHORIZATION, required = true, example = "Bearer {accessToken}")
    ResponseEntity<com.book.common.response.ApiResponse<PointResponse>> getPoint(@Parameter(hidden = true) final Long userId);
}
