package com.book.core.user.api.spec;

import com.book.core.user.api.request.UpdateMyInfoRequest;
import com.book.core.user.api.response.GetMyInfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

    @Operation(summary = "내 정보 수정", description = "인증된 회원의 닉네임을 수정합니다. 닉네임은 앞뒤 공백을 제외하고 2자 이상 20자 이하이며 중복을 허용합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "내 정보 수정 성공"),
        @ApiResponse(responseCode = "400", description = "요청 형식 또는 닉네임 정책이 올바르지 않음"),
        @ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않거나 비활성 회원"),
        @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음"), @ApiResponse(responseCode = "500", description = "알 수 없는 오류")})
    @Parameter(in = ParameterIn.HEADER, name = HttpHeaders.AUTHORIZATION, required = true, example = "Bearer {accessToken}")
    ResponseEntity<com.book.common.response.ApiResponse<Void>> updateMyInfo(@Parameter(hidden = true) final Long userId,
        @RequestBody(description = "수정할 회원 정보", required = true,
            content = @Content(schema = @Schema(implementation = UpdateMyInfoRequest.class)))
        @Valid final UpdateMyInfoRequest request);

    @Operation(summary = "회원 탈퇴 신청", description = "인증된 회원의 탈퇴를 신청하고 모든 Refresh Session을 폐기합니다. 7일 이내 카카오 로그인 시 기존 회원으로 복구되며, 이후 최종 탈퇴됩니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "회원 탈퇴 신청 성공"),
        @ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않거나 탈퇴 신청·비활성 회원"),
        @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음"), @ApiResponse(responseCode = "500", description = "알 수 없는 오류")})
    @Parameter(in = ParameterIn.HEADER, name = HttpHeaders.AUTHORIZATION, required = true, example = "Bearer {accessToken}")
    ResponseEntity<com.book.common.response.ApiResponse<Void>> requestWithdrawal(@Parameter(hidden = true) final Long userId);
}
