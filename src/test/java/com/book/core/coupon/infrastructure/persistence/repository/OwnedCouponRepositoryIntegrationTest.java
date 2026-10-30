package com.book.core.coupon.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.coupon.application.port.OwnedCouponRepositoryPort;
import com.book.core.coupon.application.usecase.IssueCouponUseCase;
import com.book.core.coupon.application.usecase.RevertOwnedCouponUseCase;
import com.book.core.coupon.application.usecase.UseOwnedCouponUseCase;
import com.book.core.coupon.domain.Coupon;
import com.book.core.coupon.domain.OwnedCoupon;
import com.book.core.coupon.domain.enums.CouponType;
import com.book.core.coupon.domain.enums.OwnedCouponStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class OwnedCouponRepositoryIntegrationTest {
    @Autowired
    CouponJpaRepository couponJpaRepository;
    @Autowired
    OwnedCouponRepositoryPort ownedCouponRepository;
    @Autowired
    OwnedCouponJpaRepository ownedCouponJpaRepository;
    @Autowired
    IssueCouponUseCase issueCoupon;
    @Autowired
    UseOwnedCouponUseCase useOwnedCoupon;
    @Autowired
    RevertOwnedCouponUseCase revertOwnedCoupon;

    @Test
    void 체크아웃은_회원과_상태와_만료만_필터하고_금액이나_횟수는_필터하지_않는다() {
        final var now = LocalDateTime.of(2026, 10, 13, 9, 0);
        final var valid = saveCoupon(now.plusDays(1));
        final var expired = saveCoupon(now);
        final var inactive = saveCoupon(now.plusDays(1));
        inactive.delete();
        couponJpaRepository.saveAndFlush(inactive);
        final var used = saveCoupon(now.plusDays(1));
        final var deleted = saveCoupon(now.plusDays(1));
        final var owned = ownedCouponJpaRepository.saveAndFlush(new OwnedCoupon(100L, valid.id(), OwnedCouponStatus.ISSUED, 2L, 2L));
        ownedCouponJpaRepository.saveAndFlush(new OwnedCoupon(100L, expired.id(), OwnedCouponStatus.ISSUED, 2L, 0L));
        ownedCouponJpaRepository.saveAndFlush(new OwnedCoupon(100L, inactive.id(), OwnedCouponStatus.ISSUED, 2L, 0L));
        ownedCouponJpaRepository.saveAndFlush(new OwnedCoupon(100L, used.id(), OwnedCouponStatus.USED, 2L, 2L));
        ownedCouponJpaRepository.saveAndFlush(new OwnedCoupon(200L, valid.id(), OwnedCouponStatus.ISSUED, 2L, 0L));
        final var deletedOwned =
            ownedCouponJpaRepository.saveAndFlush(new OwnedCoupon(100L, deleted.id(), OwnedCouponStatus.ISSUED, 2L, 0L));
        deletedOwned.delete();
        ownedCouponJpaRepository.saveAndFlush(deletedOwned);
        assertThat(
            ownedCouponRepository.findOwnedCouponIds(100L, List.of(valid.id(), expired.id(), inactive.id(), used.id(), deleted.id()), now))
            .extracting(OwnedCoupon::id).containsExactly(owned.id());
        assertThat(ownedCouponRepository.findActiveByUserId(100L)).extracting(OwnedCoupon::couponId).containsExactlyInAnyOrder(valid.id(),
            expired.id(), inactive.id(), used.id());
    }

    @Test
    void 발급_사용_복원과_삭제_후에도_유지되는_중복_제약을_검증한다() {
        final var coupon = saveCoupon(LocalDateTime.now().plusDays(7));
        issueCoupon.execute(42L, coupon.id());
        final var owned = ownedCouponRepository.findByUserIdAndCouponId(42L, coupon.id()).orElseThrow();
        useOwnedCoupon.execute(owned.id());
        useOwnedCoupon.execute(owned.id());
        assertThat(ownedCouponRepository.findById(owned.id()).orElseThrow().status()).isEqualTo(OwnedCouponStatus.USED);
        revertOwnedCoupon.execute(owned.id());
        assertThat(ownedCouponRepository.findById(owned.id()).orElseThrow().status()).isEqualTo(OwnedCouponStatus.USED);
        revertOwnedCoupon.execute(owned.id());
        final var restored = ownedCouponRepository.findById(owned.id()).orElseThrow();
        assertThat(restored.usedCount()).isZero();
        assertThat(restored.status()).isEqualTo(OwnedCouponStatus.ISSUED);
        restored.delete();
        ownedCouponRepository.save(restored);
        assertThatThrownBy(() -> issueCoupon.execute(42L, coupon.id())).isInstanceOf(CoreException.class).extracting("errorCode")
            .isEqualTo(ErrorCode.COUPON_ALREADY_ISSUED);
        assertThatThrownBy(() -> ownedCouponJpaRepository.saveAndFlush(new OwnedCoupon(42L, coupon.id(), OwnedCouponStatus.ISSUED, 2L, 0L)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Coupon saveCoupon(final LocalDateTime expiredAt) {
        return couponJpaRepository.saveAndFlush(new Coupon(null, "쿠폰", CouponType.FIXED_AMOUNT, BigDecimal.TEN, 2L,
            new BigDecimal("1000000"), expiredAt, 100, 0, expiredAt.minusDays(10)));
    }
}
