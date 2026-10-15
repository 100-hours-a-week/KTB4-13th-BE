package com.book.core.coupon.api.spec;

import com.book.core.coupon.api.response.OwnedCouponResponse;
import com.book.core.coupon.api.response.CouponResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import org.springframework.http.ResponseEntity;

@Tag(name = "Coupon", description = "쿠폰 API")
public interface CouponControllerSpec {
    @Operation(summary = "쿠폰 조회", description = "쿠폰 ID 목록으로 활성 쿠폰을 조회합니다. 만료 쿠폰도 포함하며, 존재하지 않거나 비활성인 ID는 결과에서 제외합니다.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "쿠폰 조회 성공"),
        @ApiResponse(responseCode = "400", description = "쿠폰 ID 목록이 누락되거나 올바르지 않음")})
    ResponseEntity<com.book.common.response.ApiResponse<List<CouponResponse>>> getCoupons(
        @Parameter(description = "쉼표로 구분한 쿠폰 ID 목록", required = true, example = "10,20") @RequestParam("ids")
        @NotEmpty final List<@NotNull @Positive Long> couponIds);

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "쿠폰 발급", description = "활성 상태이며 만료되지 않은 쿠폰을 인증된 회원에게 발급합니다.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "쿠폰 발급 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 쿠폰 ID, 만료 쿠폰 또는 이미 발급한 쿠폰"),
        @ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않음")})
    ResponseEntity<com.book.common.response.ApiResponse<Void>> issue(@Parameter(hidden = true) final Long userId,
        @Parameter(description = "쿠폰 ID", required = true) @Positive final Long couponId);

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "보유 쿠폰 조회", description = "인증된 회원의 활성 보유 쿠폰을 조회합니다. 만료 및 사용 완료 쿠폰도 포함합니다.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "보유 쿠폰 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않음")})
    ResponseEntity<com.book.common.response.ApiResponse<List<OwnedCouponResponse>>> getOwnedCoupons(
        @Parameter(hidden = true) final Long userId);
}
