package com.book.core.coupon.api.spec;

import com.book.core.coupon.api.response.WaitingRoomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;

@Tag(name = "Waiting Room", description = "행사 쿠폰 대기실 API")
@SecurityRequirement(name = "bearerAuth")
public interface WaitingRoomControllerSpec {
    @Operation(summary = "대기실 진입", description = "행사 쿠폰 대기열에 등록하고 현재 순번과 예상 대기 시간을 반환합니다.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "대기실 상태 반환"),
        @ApiResponse(responseCode = "404", description = "설정된 행사 쿠폰 대기실이 없음"),
        @ApiResponse(responseCode = "429", description = "사용자별 요청 한도 초과", headers = @Header(name = "Retry-After"))})
    com.book.common.response.ApiResponse<WaitingRoomResponse> enter(@Positive long couponId, @Parameter(hidden = true) Long userId);

    @Operation(summary = "대기실 조회", description = "순번과 입장권 유효 여부를 반환합니다.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "대기실 상태 반환"),
        @ApiResponse(responseCode = "404", description = "대기실에 들어오지 않았거나 입장권이 만료됨"),
        @ApiResponse(responseCode = "429", description = "사용자별 요청 한도 초과", headers = @Header(name = "Retry-After"))})
    com.book.common.response.ApiResponse<WaitingRoomResponse> status(@Positive long couponId, @Parameter(hidden = true) Long userId);
}
