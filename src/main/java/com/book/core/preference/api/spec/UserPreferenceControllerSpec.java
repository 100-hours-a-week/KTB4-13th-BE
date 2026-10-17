package com.book.core.preference.api.spec;

import com.book.common.response.ApiResponse;
import com.book.core.preference.api.response.UserPreferencesResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "User Preference", description = "회원 취향 조회와 취향 기억 삭제 API")
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

    @Operation(summary = "취향 기억 선택 삭제",
        description = "ids로 전달한 본인의 취향 기억을 삭제합니다. 중복 ID는 한 번만 처리하고, 이미 삭제한 기억은 성공으로 처리합니다. "
            + "존재하지 않거나 다른 회원의 기억이 하나라도 있으면 아무것도 삭제하지 않고 404를 응답합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "취향 기억 삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "ids가 없거나 양수가 아닌 값이 포함됨"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "온보딩 정보가 없거나 삭제할 수 없는 취향 기억이 포함됨"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "온보딩을 완료하지 않음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "알 수 없는 오류")})
    @Parameter(in = ParameterIn.HEADER, name = HttpHeaders.AUTHORIZATION, required = true, example = "Bearer {accessToken}")
    ResponseEntity<Void> deleteMemories(@Parameter(hidden = true) final Long userId,
        @Parameter(in = ParameterIn.QUERY, name = "ids", required = true, example = "101,102") @NotEmpty
        @RequestParam("ids") final List<@NotNull @Positive Long> memoryIds);

    @Operation(summary = "취향 기억 전체 삭제", description = "본인의 활성 취향 기억을 모두 삭제합니다. 온보딩 취향은 삭제하지 않으며, 삭제할 기억이 없어도 성공으로 처리합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "취향 기억 전체 삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "온보딩 정보가 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "온보딩을 완료하지 않음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "알 수 없는 오류")})
    @Parameter(in = ParameterIn.HEADER, name = HttpHeaders.AUTHORIZATION, required = true, example = "Bearer {accessToken}")
    ResponseEntity<Void> deleteAllMemories(@Parameter(hidden = true) final Long userId);
}
