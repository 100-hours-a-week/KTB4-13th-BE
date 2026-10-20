package com.book.core.point.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.point.application.port.PointBalanceRepositoryPort;
import com.book.core.point.application.port.PointHistoryRepositoryPort;
import com.book.core.point.application.port.PointLotRepositoryPort;
import com.book.core.point.domain.PointBalance;
import com.book.core.point.domain.PointHistory;
import com.book.core.point.domain.PointLot;
import com.book.core.point.domain.enums.PointType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public class EarnPointUseCase {
    private final PointBalanceRepositoryPort pointBalanceRepository;
    private final PointHistoryRepositoryPort pointHistoryRepository;
    private final PointLotRepositoryPort pointLotRepository;
    private final Clock clock;

    public EarnPointUseCase(final PointBalanceRepositoryPort pointBalanceRepository,
        final PointHistoryRepositoryPort pointHistoryRepository) {
        this(pointBalanceRepository, pointHistoryRepository, null, Clock.systemDefaultZone());
    }

    @Transactional
    public void execute(final Long userId, final PointType type, final Long referenceId, final BigDecimal amount) {
        execute(userId, type, referenceId, amount, "point-earned:" + type + ":" + referenceId);
    }

    @Transactional
    public void execute(final Long userId, final PointType type, final Long referenceId, final BigDecimal amount, final String eventKey) {
        if (eventKey == null || eventKey.isBlank()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (amount.signum() == 0) {
            log.debug("금액이 0이어서 포인트 적립을 생략합니다. type={}, referenceId={}, traceId={}", type, referenceId, MDC.get("traceId"));
            return;
        }
        if (amount.signum() < 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }

        log.debug("포인트 적립 처리를 시작합니다. type={}, referenceId={}, traceId={}", type, referenceId, MDC.get("traceId"));

        final PointBalance balance =
            pointBalanceRepository.findByUserIdForUpdate(userId).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
        if (pointHistoryRepository.findByUserIdAndEventKey(userId, eventKey).isPresent()) {
            return;
        }
        final BigDecimal previousBalance = balance.balance();
        balance.apply(amount);
        final PointHistory history = PointHistory.builder().userId(userId).type(type).referenceId(referenceId).amount(amount)
            .balanceAfter(balance.balance()).build().withEventKey(eventKey);
        final PointHistory savedHistory = pointHistoryRepository.save(history);
        final BigDecimal pointLotAmount = amount.add(previousBalance.min(BigDecimal.ZERO)).max(BigDecimal.ZERO);
        if (pointLotRepository != null && pointLotAmount.signum() > 0) {
            pointLotRepository.save(new PointLot(userId, savedHistory.id(), LocalDateTime.now(clock).plusYears(1), amount, pointLotAmount));
        }
        log.info("포인트 적립 처리를 마쳤습니다. type={}, referenceId={}, traceId={}", type, referenceId, MDC.get("traceId"));
    }
}
