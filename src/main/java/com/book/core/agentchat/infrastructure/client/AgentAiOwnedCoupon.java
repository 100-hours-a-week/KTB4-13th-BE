package com.book.core.agentchat.infrastructure.client;

import com.book.core.coupon.application.result.CouponResult;
import com.book.core.coupon.application.result.OwnedCouponResult;
import com.book.core.coupon.domain.enums.CouponType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

record AgentAiOwnedCoupon(@JsonProperty("owned_coupon_id") Long ownedCouponId, @JsonProperty("coupon_id") Long couponId, String name,
    CouponType type, BigDecimal discount, @JsonProperty("min_order_amount") BigDecimal minOrderAmount,
    @JsonProperty("expired_at") String expiredAt) {
    static AgentAiOwnedCoupon from(final OwnedCouponResult owned) {
        final CouponResult coupon = owned.coupon();
        return new AgentAiOwnedCoupon(owned.id(), coupon.id(), coupon.name(), coupon.type(), coupon.discount(), coupon.minOrderAmount(),
            coupon.expiredAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }
}
