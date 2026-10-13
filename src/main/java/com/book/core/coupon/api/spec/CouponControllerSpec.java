package com.book.core.coupon.api.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;

@Tag(name = "Coupon", description = "쿠폰 API")
@SecurityRequirement(name = "bearerAuth")
public interface CouponControllerSpec {
    @Operation(summary = "쿠폰 발급", description = "활성 상태이며 만료되지 않은 쿠폰을 인증된 회원에게 발급합니다.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "쿠폰 발급 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 쿠폰 ID, 만료 쿠폰 또는 이미 발급한 쿠폰"),
        @ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않음")})
    ResponseEntity<com.book.common.response.ApiResponse<Void>> issue(@Parameter(hidden = true) final Long userId,
        @Parameter(description = "쿠폰 ID", required = true) @Positive final Long couponId);

}
