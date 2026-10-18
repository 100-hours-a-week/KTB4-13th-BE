package com.book.core.coupon.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.coupon.domain.enums.OwnedCouponStatus;
import org.junit.jupiter.api.Test;

class OwnedCouponTest {
    @Test
    void 사용_한도에_도달할_때만_USED로_전환한다() {
        final var ownedCoupon = new OwnedCoupon(42L, 10L, OwnedCouponStatus.ISSUED, 2L);
        assertThat(ownedCoupon.usedCount()).isZero();
        assertThat(ownedCoupon.isUnused()).isTrue();
        assertThat(ownedCoupon.isFullyUsed()).isFalse();
        ownedCoupon.use();
        assertThat(ownedCoupon.usedCount()).isEqualTo(1L);
        assertThat(ownedCoupon.status()).isEqualTo(OwnedCouponStatus.ISSUED);
        assertThat(ownedCoupon.isUnused()).isFalse();
        assertThat(ownedCoupon.isFullyUsed()).isFalse();
        ownedCoupon.use();
        assertThat(ownedCoupon.usedCount()).isEqualTo(2L);
        assertThat(ownedCoupon.status()).isEqualTo(OwnedCouponStatus.USED);
        assertThat(ownedCoupon.isFullyUsed()).isTrue();
    }

    @Test
    void 전부_복원되어야_사용횟수_0과_ISSUED로_돌아간다() {
        final var ownedCoupon = new OwnedCoupon(42L, 10L, OwnedCouponStatus.USED, 2L, 2L);
        ownedCoupon.revert();
        assertThat(ownedCoupon.usedCount()).isEqualTo(1L);
        assertThat(ownedCoupon.status()).isEqualTo(OwnedCouponStatus.USED);
        assertThat(ownedCoupon.isUnused()).isFalse();
        ownedCoupon.revert();
        assertThat(ownedCoupon.usedCount()).isZero();
        assertThat(ownedCoupon.status()).isEqualTo(OwnedCouponStatus.ISSUED);
        assertThat(ownedCoupon.isUnused()).isTrue();
    }
}
