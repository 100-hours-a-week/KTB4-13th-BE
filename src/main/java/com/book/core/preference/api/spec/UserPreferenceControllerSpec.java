package com.book.core.preference.api.spec;

import com.book.common.response.ApiResponse;
import com.book.core.preference.api.response.UserPreferencesResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

@Tag(name = "User Preference", description = "회원 취향 API")
public interface UserPreferenceControllerSpec {
    @Operation(summary = "내 취향 조회",
        description = "온보딩에서 선택한 취향과 활성 AI 취향 기억을 함께 조회합니다. " + "취향 기억은 종류와 정규화한 핵심어가 같으면 하나로 묶고, 최신 기억의 문장을 대표값으로 최신순 반환합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "내 취향 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "온보딩 정보가 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "온보딩을 완료하지 않음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "알 수 없는 오류")})
    @Parameter(in = ParameterIn.HEADER, name = HttpHeaders.AUTHORIZATION, required = true, example = "Bearer {accessToken}")
    ResponseEntity<ApiResponse<UserPreferencesResponse>> getPreferences(@Parameter(hidden = true) final Long userId);
}
