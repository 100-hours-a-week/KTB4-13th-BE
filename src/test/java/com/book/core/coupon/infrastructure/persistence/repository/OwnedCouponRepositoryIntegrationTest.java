package com.book.core.coupon.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.coupon.application.port.OwnedCouponRepositoryPort;
import com.book.core.coupon.application.usecase.IssueCouponUseCase;
import com.book.core.coupon.domain.Coupon;
import com.book.core.coupon.domain.OwnedCoupon;
import com.book.core.coupon.domain.enums.CouponType;
import com.book.core.coupon.domain.enums.OwnedCouponStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
class OwnedCouponRepositoryIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    CouponJpaRepository couponJpaRepository;
    @Autowired
    OwnedCouponRepositoryPort ownedCouponRepository;
    @Autowired
    OwnedCouponJpaRepository ownedCouponJpaRepository;
    @Autowired
    IssueCouponUseCase issueCoupon;



    @Test
    void 발급과_삭제_후에도_유지되는_중복_제약을_검증한다() {
        final var coupon = saveCoupon(LocalDateTime.now().plusDays(7));
        issueCoupon.execute(42L, coupon.id());
        final var owned = ownedCouponRepository.findByUserIdAndCouponId(42L, coupon.id()).orElseThrow();
        owned.delete();
        ownedCouponRepository.save(owned);
        assertThatThrownBy(() -> issueCoupon.execute(42L, coupon.id())).isInstanceOf(CoreException.class).extracting("errorCode")
            .isEqualTo(ErrorCode.COUPON_ALREADY_DOWNLOADED);
        assertThatThrownBy(() -> ownedCouponJpaRepository.saveAndFlush(new OwnedCoupon(42L, coupon.id(), OwnedCouponStatus.ISSUED, 2L, 0L)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Coupon saveCoupon(final LocalDateTime expiredAt) {
        return couponJpaRepository
            .saveAndFlush(new Coupon(null, "쿠폰", CouponType.FIXED_AMOUNT, BigDecimal.TEN, 2L, new BigDecimal("1000000"), expiredAt));
    }
}
