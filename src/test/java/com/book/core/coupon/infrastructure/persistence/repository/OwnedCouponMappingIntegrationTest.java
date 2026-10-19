package com.book.core.coupon.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.core.coupon.application.port.OwnedCouponRepositoryPort;
import com.book.core.coupon.domain.OwnedCoupon;
import com.book.core.coupon.domain.enums.OwnedCouponStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles({"test", "mysql"})
@Testcontainers
@Tag("integration")
class OwnedCouponMappingIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    OwnedCouponRepositoryPort ownedCoupons;

    @Autowired
    OwnedCouponJpaRepository ownedCouponJpaRepository;

    @Test
    void 보유_쿠폰을_복원하고_다른_회원의_같은_쿠폰은_허용한다() {
        final var saved = ownedCoupons.save(new OwnedCoupon(42L, 10L, OwnedCouponStatus.ISSUED, 2L, 0L));
        final var restored = ownedCoupons.findByUserIdAndCouponId(42L, 10L).orElseThrow();

        assertThat(saved.id()).isNotNull();
        assertThat(restored.id()).isEqualTo(saved.id());
        assertThat(restored.status()).isEqualTo(OwnedCouponStatus.ISSUED);
        assertThat(restored.maxUseCount()).isEqualTo(2L);
        assertThat(restored.usedCount()).isZero();
        assertThat(ownedCoupons.findByUserIdAndCouponId(999L, 10L)).isEmpty();
        assertThat(ownedCoupons.save(new OwnedCoupon(43L, 10L, OwnedCouponStatus.ISSUED, 2L, 0L)).id()).isNotEqualTo(saved.id());
    }

    @Test
    void 삭제_후에도_회원과_쿠폰의_고유_제약을_유지한다() {
        final var saved = ownedCoupons.save(new OwnedCoupon(100L, 20L, OwnedCouponStatus.ISSUED, 2L, 0L));
        assertDuplicate();

        saved.delete();
        ownedCoupons.save(saved);

        assertThat(ownedCoupons.findByUserIdAndCouponId(100L, 20L).orElseThrow().deletedAt()).isNotNull();
        assertDuplicate();
    }

    private void assertDuplicate() {
        assertThatThrownBy(() -> ownedCouponJpaRepository.saveAndFlush(new OwnedCoupon(100L, 20L, OwnedCouponStatus.ISSUED, 2L, 0L)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}
