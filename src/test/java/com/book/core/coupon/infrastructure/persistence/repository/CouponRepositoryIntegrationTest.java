package com.book.core.coupon.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.coupon.application.port.CouponRepositoryPort;
import com.book.core.coupon.domain.Coupon;
import com.book.core.coupon.domain.enums.CouponType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class CouponRepositoryIntegrationTest {
    @Autowired
    CouponRepositoryPort couponRepository;
    @Autowired
    CouponJpaRepository couponJpaRepository;

    @Test
    void 마이그레이션과_매핑은_소수_할인율과_만료_경계를_보존한다() {
        final var now = LocalDateTime.of(2026, 10, 13, 9, 0);
        final var valid = couponJpaRepository.saveAndFlush(
            new Coupon(null, "10퍼센트", CouponType.PERCENT_RATE, new BigDecimal("0.10"), 2L, new BigDecimal("1000"), now.plusDays(1)));
        final var expired =
            couponJpaRepository.saveAndFlush(new Coupon(null, "만료", CouponType.FIXED_AMOUNT, BigDecimal.TEN, 2L, BigDecimal.ZERO, now));
        final var deleted = couponJpaRepository
            .saveAndFlush(new Coupon(null, "삭제", CouponType.NONE, BigDecimal.ZERO, 2L, BigDecimal.ZERO, now.plusDays(1)));
        deleted.delete();
        couponJpaRepository.saveAndFlush(deleted);
        assertThat(couponRepository.findIssuableById(valid.id(), now)).isPresent();
        assertThat(couponRepository.findIssuableById(expired.id(), now)).isEmpty();
        assertThat(couponRepository.findIssuableById(deleted.id(), now)).isEmpty();
        assertThat(couponRepository.findActiveByIds(List.of(valid.id(), expired.id(), deleted.id()))).extracting(Coupon::id)
            .containsExactlyInAnyOrder(valid.id(), expired.id());
        assertThat(couponRepository.findById(valid.id()).orElseThrow().discount()).isEqualByComparingTo("0.10");
    }
}
