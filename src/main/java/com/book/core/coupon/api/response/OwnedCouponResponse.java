package com.book.core.coupon.api.response;

import com.book.core.coupon.domain.enums.CouponType;
import com.book.core.coupon.domain.enums.OwnedCouponStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OwnedCouponResponse(@Schema(description = "발급된 보유 쿠폰 ID. 결제 요청의 useOwnedCouponId에 사용합니다.") Long id,
    @Schema(description = "쿠폰 정책 ID. 상품 상세의 couponIds와 비교합니다.") Long couponId, OwnedCouponStatus state, String name, CouponType type,
    BigDecimal discount, BigDecimal minOrderAmount, long maxUseCount, long usedCount, LocalDateTime expiredAt) {
}
