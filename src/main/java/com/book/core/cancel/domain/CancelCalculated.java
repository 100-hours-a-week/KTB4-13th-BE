package com.book.core.cancel.domain;

import java.math.BigDecimal;

/** 참고 계산 결과의 금액과 쿠폰 복원 여부입니다. */
public record CancelCalculated(BigDecimal paidAmount, BigDecimal couponAmount, BigDecimal pointAmount, boolean isRestoreCoupon) {
}
