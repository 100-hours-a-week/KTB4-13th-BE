package com.book.core.coupon.api.spec;

import com.book.core.coupon.api.request.CreateCouponRequest;
import com.book.core.coupon.api.response.CouponResponse;
import com.book.core.coupon.api.response.OwnedCouponResponse;
import com.book.core.coupon.application.result.CouponWaitingRoomStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Coupon", description = "쿠폰 API")
public interface CouponControllerSpec {
    @Operation(summary = "쿠폰 생성", description = "발급에 필요한 정보로 쿠폰을 생성합니다. 생성된 쿠폰 정보를 반환합니다.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "쿠폰 생성 성공"),
        @ApiResponse(responseCode = "400", description = "쿠폰 요청 값이 올바르지 않음")})
    ResponseEntity<com.book.common.response.ApiResponse<CouponResponse>> create(@Valid final CreateCouponRequest request);

    @Operation(summary = "쿠폰 조회", description = "쿠폰 ID 목록으로 활성 쿠폰을 조회합니다. 만료 쿠폰도 포함하며, 존재하지 않거나 비활성인 ID는 결과에서 제외합니다.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "쿠폰 조회 성공"),
        @ApiResponse(responseCode = "400", description = "쿠폰 ID 목록이 누락되거나 올바르지 않음")})
    ResponseEntity<com.book.common.response.ApiResponse<List<CouponResponse>>> getCoupons(
        @Parameter(description = "쉼표로 구분한 쿠폰 ID 목록", required = true, example = "10,20") @RequestParam("ids")
        @NotEmpty final List<@NotNull @Positive Long> couponIds);

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "쿠폰 발급", description = "활성 상태이고 발급 기간과 잔여 수량이 유효한 쿠폰을 인증된 회원에게 발급합니다.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "쿠폰 발급 및 DB 저장 완료"),
        @ApiResponse(responseCode = "202", description = "Streams 실험 모드에서 접수 완료. 쿠폰함에는 DB 저장 이후 반영됩니다."),
        @ApiResponse(responseCode = "400", description = "잘못된 쿠폰 ID, 발급 기간이 아닌 쿠폰(E4000), 이미 발급한 쿠폰(E4001) 또는 발급 한도 초과(E4003)"),
        @ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않음"),
        @ApiResponse(responseCode = "409", description = "대기열 대상 쿠폰의 유효한 입장권이 없음(E4005)")})
    ResponseEntity<com.book.common.response.ApiResponse<Void>> issue(@Parameter(hidden = true) final Long userId,
        @Parameter(description = "쿠폰 ID", required = true) @Positive final Long couponId);

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "쿠폰 대기열 등록", description = "현재 순번과 예상 대기 시간을 반환합니다. 입장권을 받은 사용자는 순번이 0입니다.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "대기열 상태 반환"),
        @ApiResponse(responseCode = "404", description = "대기열이 적용된 쿠폰이 아님(E4004)")})
    ResponseEntity<com.book.common.response.ApiResponse<CouponWaitingRoomStatus>> enterWaitingRoom(
        @Parameter(hidden = true) final Long userId, @Positive final Long couponId);

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "쿠폰 대기열 조회", description = "현재 순번과 입장 가능 여부를 조회합니다.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "대기열 상태 반환"),
        @ApiResponse(responseCode = "404", description = "대기열이 적용된 쿠폰이 아님(E4004)"),
        @ApiResponse(responseCode = "409", description = "대기열에 등록하지 않았거나 입장권이 만료됨(E4005)")})
    ResponseEntity<com.book.common.response.ApiResponse<CouponWaitingRoomStatus>> getWaitingRoomStatus(
        @Parameter(hidden = true) final Long userId, @Positive final Long couponId);

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "보유 쿠폰 조회", description = "인증된 회원의 활성 보유 쿠폰을 조회합니다. 만료 및 사용 완료 쿠폰도 포함합니다.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "보유 쿠폰 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않음")})
    ResponseEntity<com.book.common.response.ApiResponse<List<OwnedCouponResponse>>> getOwnedCoupons(
        @Parameter(hidden = true) final Long userId);
}
