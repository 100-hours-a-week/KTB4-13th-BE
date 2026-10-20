package com.book.core.point.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.point.application.port.PointBalanceRepositoryPort;
import com.book.core.point.application.port.PointHistoryRepositoryPort;
import com.book.core.point.application.port.PointLotAllocationRepositoryPort;
import com.book.core.point.application.port.PointLotRepositoryPort;
import com.book.core.point.domain.PointBalance;
import com.book.core.point.domain.PointHistory;
import com.book.core.point.domain.PointLot;
import com.book.core.point.domain.PointLotAllocation;
import com.book.core.point.domain.enums.PointType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public class DeductPointUseCase {
    private final PointBalanceRepositoryPort pointBalanceRepository;
    private final PointHistoryRepositoryPort pointHistoryRepository;
    private final PointLotRepositoryPort pointLotRepository;
    private final PointLotAllocationRepositoryPort pointLotAllocationRepository;
    private final Clock clock;

    public DeductPointUseCase(final PointBalanceRepositoryPort pointBalanceRepository,
        final PointHistoryRepositoryPort pointHistoryRepository) {
        this(pointBalanceRepository, pointHistoryRepository, null, null, Clock.systemDefaultZone());
    }

    @Transactional
    public void execute(final Long userId, final PointType type, final Long referenceId, final BigDecimal amount) {
        execute(userId, type, referenceId, amount, Map.of());
    }

    @Transactional
    public void execute(final Long userId, final PointType type, final Long referenceId, final BigDecimal amount,
        final Map<Long, BigDecimal> orderItemAmounts) {
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            log.debug("금액이 0이어서 포인트 차감을 생략합니다. type={}, referenceId={}, traceId={}", type, referenceId, MDC.get("traceId"));
            return;
        }
        if (amount.signum() < 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        log.debug("포인트 차감 처리를 시작합니다. type={}, referenceId={}, traceId={}", type, referenceId, MDC.get("traceId"));

        final PointBalance balance =
            pointBalanceRepository.findByUserIdForUpdate(userId).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
        if (pointLotRepository == null) {
            if (type == PointType.PAYMENT && amount.compareTo(balance.balance()) > 0) {
                throw new CoreException(ErrorCode.POINT_EXCEEDS_BALANCE);
            }
            balance.apply(amount.negate());
            pointHistoryRepository.save(PointHistory.builder().userId(userId).type(type).referenceId(referenceId).amount(amount.negate())
                .balanceAfter(balance.balance()).build());
            return;
        }
        if (type == PointType.PAYMENT && amount.compareTo(balance.balance()) > 0) {
            throw new CoreException(ErrorCode.POINT_EXCEEDS_BALANCE);
        }
        if (type == PointType.PAYMENT && !orderItemAmounts.isEmpty()
            && orderItemAmounts.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(amount) != 0) {
            throw new CoreException(ErrorCode.POINT_DATA_INCONSISTENT);
        }
        final LocalDateTime now = LocalDateTime.now(clock);
        final var availableLots = pointLotRepository.findAvailableByUserIdForUpdate(userId, now);
        final BigDecimal availableLotBalance =
            availableLots.stream().map(PointLot::remainingAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (type == PointType.PAYMENT && availableLotBalance.compareTo(amount) < 0) {
            throw new CoreException(ErrorCode.POINT_DATA_INCONSISTENT);
        }
        final BigDecimal amountToConsume = amount.min(balance.balance().max(BigDecimal.ZERO)).min(availableLotBalance);
        final List<AllocationDraft> allocations = new ArrayList<>();
        if (type == PointType.PAYMENT && !orderItemAmounts.isEmpty()) {
            int lotIndex = 0;
            BigDecimal remainingInLot = availableLots.isEmpty() ? BigDecimal.ZERO : availableLots.getFirst().remainingAmount();
            for (final Map.Entry<Long, BigDecimal> itemEntry : orderItemAmounts.entrySet()) {
                BigDecimal remainingForItem = itemEntry.getValue();
                while (remainingForItem.signum() > 0) {
                    if (lotIndex >= availableLots.size()) {
                        throw new CoreException(ErrorCode.POINT_DATA_INCONSISTENT);
                    }
                    final PointLot pointLot = availableLots.get(lotIndex);
                    final BigDecimal consumedAmount = remainingForItem.min(remainingInLot);
                    pointLot.consume(consumedAmount);
                    allocations.add(new AllocationDraft(pointLot.id(), itemEntry.getKey(), consumedAmount));
                    remainingForItem = remainingForItem.subtract(consumedAmount);
                    remainingInLot = remainingInLot.subtract(consumedAmount);
                    if (remainingInLot.signum() == 0) {
                        lotIndex++;
                        if (lotIndex < availableLots.size()) {
                            remainingInLot = availableLots.get(lotIndex).remainingAmount();
                        }
                    }
                }
            }
        } else {
            BigDecimal remainingToConsume = amountToConsume;
            for (final PointLot pointLot : availableLots) {
                if (remainingToConsume.signum() == 0) {
                    break;
                }
                final BigDecimal consumedAmount = pointLot.remainingAmount().min(remainingToConsume);
                pointLot.consume(consumedAmount);
                if (type == PointType.PAYMENT && consumedAmount.signum() > 0) {
                    allocations.add(new AllocationDraft(pointLot.id(), null, consumedAmount));
                }
                remainingToConsume = remainingToConsume.subtract(consumedAmount);
            }
            if (remainingToConsume.signum() > 0) {
                throw new CoreException(ErrorCode.POINT_DATA_INCONSISTENT);
            }
        }
        final BigDecimal deductedAmount = amount.negate();
        balance.apply(deductedAmount);
        final PointHistory history = PointHistory.builder().userId(userId).type(type).referenceId(referenceId).amount(deductedAmount)
            .balanceAfter(balance.balance()).build();
        final PointHistory savedHistory = pointHistoryRepository.save(history);
        if (!allocations.isEmpty()) {
            final var savedAllocations =
                allocations.stream().map((final AllocationDraft allocation) -> new PointLotAllocation(savedHistory.id(),
                    allocation.pointLotId(), allocation.orderItemId(), allocation.amount())).toList();
            pointLotAllocationRepository.saveAll(savedAllocations);
        }
        log.info("포인트 차감 처리를 마쳤습니다. type={}, referenceId={}, traceId={}", type, referenceId, MDC.get("traceId"));
    }

    private record AllocationDraft(Long pointLotId, Long orderItemId, BigDecimal amount) {
    }
}
