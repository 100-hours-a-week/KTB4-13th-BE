package com.book.core.user.api.spec;

import com.book.core.user.api.response.GetMyInfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

@Tag(name = "User", description = "회원 API")
public interface UserControllerSpec {
    @Operation(summary = "내 정보 조회", description = "인증된 회원의 닉네임을 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "내 정보 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않거나 비활성 회원"),
        @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음"), @ApiResponse(responseCode = "500", description = "알 수 없는 오류")})
    @Parameter(in = ParameterIn.HEADER, name = HttpHeaders.AUTHORIZATION, required = true, example = "Bearer {accessToken}")
    ResponseEntity<com.book.common.response.ApiResponse<GetMyInfoResponse>> getMyInfo(@Parameter(hidden = true) final Long userId);
}
