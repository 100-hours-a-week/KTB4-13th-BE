package com.book.core.point.application.service;

import com.book.core.point.application.command.DeductPointCommand;
import com.book.core.point.application.command.EarnPointCommand;
import com.book.core.point.application.command.GetPointCommand;
import com.book.core.point.application.result.GetPointResult;
import com.book.core.point.application.usecase.DeductPointUseCase;
import com.book.core.point.application.usecase.EarnPointUseCase;
import com.book.core.point.application.usecase.FindUserIdsWithoutPointBalanceUseCase;
import com.book.core.point.application.usecase.GetPointUseCase;
import com.book.core.point.application.usecase.RecoverPointBalanceUseCase;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointService {
    private final GetPointUseCase getPointUseCase;
    private final EarnPointUseCase earnPointUseCase;
    private final DeductPointUseCase deductPointUseCase;
    private final FindUserIdsWithoutPointBalanceUseCase findUserIdsWithoutPointBalanceUseCase;
    private final RecoverPointBalanceUseCase recoverPointBalanceUseCase;

    public GetPointResult getPoint(final GetPointCommand command) {
        return getPointUseCase.execute(command);
    }

    public void earn(final EarnPointCommand command) {
        earnPointUseCase.execute(command.userId(), command.type(), command.referenceId(), command.amount());
    }

    public void deduct(final DeductPointCommand command) {
        deductPointUseCase.execute(command.userId(), command.type(), command.referenceId(), command.amount());
    }

    public void recoverMissingPointBalances(final LocalDateTime registeredBefore, final int pageSize) {
        int target = 0;
        int recovered = 0;
        int skipped = 0;
        final Map<String, Integer> failureCauses = new TreeMap<>();
        Long lastUserId = 0L;
        while (true) {
            final List<Long> userIds = findUserIdsWithoutPointBalanceUseCase.execute(registeredBefore, lastUserId, pageSize);
            for (final Long userId : userIds) {
                try {
                    if (recoverPointBalanceUseCase.execute(userId)) {
                        recovered++;
                    } else {
                        skipped++;
                    }
                } catch (final RuntimeException exception) {
                    failureCauses.merge(exception.getClass().getSimpleName(), 1, Integer::sum);
                    log.warn("포인트 잔액 누락 복구에 실패했습니다. userId={}, cause={}", userId, exception.getClass().getSimpleName(), exception);
                }
            }
            target += userIds.size();
            if (userIds.size() < pageSize) {
                break;
            }
            lastUserId = userIds.getLast();
        }
        log.info("포인트 잔액 누락 복구를 마쳤습니다. target={}, recovered={}, skipped={}, failed={}, failureCauses={}", target, recovered, skipped,
            target - recovered - skipped, failureCauses);
    }
}
