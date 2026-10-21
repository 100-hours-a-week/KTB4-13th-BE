package com.book.core.coupon.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.coupon.application.port.OwnedCouponRepositoryPort;
import com.book.core.coupon.domain.OwnedCoupon;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class RevertOwnedCouponUseCase {
    private final OwnedCouponRepositoryPort ownedCouponRepository;

    /**
     * 보유 쿠폰의 사용 횟수를 감소시킨다.
     *
     * @apiNote 사용처를 연결할 때 낙관적 락을 적용하고, 실제 트랜잭션 호출 바깥에서 충돌을 처리해야 한다. 충돌은 조회 이후 다른 트랜잭션이 같은 보유 쿠폰을 먼저
     *          변경했음을 뜻하며, 중복 복원 방지는 별도로 보장해야 한다.
     */
    @Transactional
    public void execute(final Long ownedCouponId) {
        log.atDebug().addKeyValue("ownedCouponId", ownedCouponId).log("[OWNED_COUPON] 쿠폰 복원 처리를 시작합니다. ownedCouponId={}", ownedCouponId);
        final OwnedCoupon ownedCoupon =
            ownedCouponRepository.findById(ownedCouponId).orElseThrow(() -> new CoreException(ErrorCode.OWNED_COUPON_INVALID));

        if (ownedCoupon.isUnused()) {
            log.atError().addKeyValue("ownedCouponId", ownedCouponId).log("[OWNED_COUPON] 비정상적인 쿠폰 복원 - 미사용 쿠폰 복원 시도 ownedCouponId={}",
                ownedCouponId);
            throw new CoreException(ErrorCode.OWNED_COUPON_INVALID_USAGE);
        }

        ownedCoupon.revert();
        log.atInfo().addKeyValue("ownedCouponId", ownedCouponId).log("[OWNED_COUPON] 쿠폰 복원 처리를 마쳤습니다. ownedCouponId={}", ownedCouponId);
    }
}
