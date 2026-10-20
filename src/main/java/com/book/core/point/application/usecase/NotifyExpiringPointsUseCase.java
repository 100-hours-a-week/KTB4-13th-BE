package com.book.core.point.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.notification.application.command.CreateNotificationCommand;
import com.book.core.notification.application.usecase.CreateNotificationUseCase;
import com.book.core.point.application.port.PointLotRepositoryPort;
import com.book.core.point.domain.PointLot;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class NotifyExpiringPointsUseCase {
    private static final int BATCH_SIZE = 1000;

    private final PointLotRepositoryPort pointLotRepository;
    private final CreateNotificationUseCase createNotificationUseCase;
    private final Clock clock;

    @Transactional
    public int execute() {
        final LocalDateTime now = LocalDateTime.now(clock);
        final var pointLots = pointLotRepository.findExpiringForUpdate(now, now.plusDays(7), BATCH_SIZE);
        final Map<ExpiryNoticeKey, BigDecimal> amountsByUserAndDate = new LinkedHashMap<>();
        for (final PointLot pointLot : pointLots) {
            final LocalDate expirationDate = pointLot.expiresAt().toLocalDate();
            final ExpiryNoticeKey key = new ExpiryNoticeKey(pointLot.userId(), expirationDate);
            amountsByUserAndDate.merge(key, pointLot.remainingAmount(), BigDecimal::add);
            pointLot.markExpiryNotificationSent(now);
        }
        for (final Map.Entry<ExpiryNoticeKey, BigDecimal> entry : amountsByUserAndDate.entrySet()) {
            final ExpiryNoticeKey key = entry.getKey();
            final String amount = entry.getValue().stripTrailingZeros().toPlainString();
            createNotificationUseCase.execute(new CreateNotificationCommand(key.userId(), "포인트 만료 예정 안내",
                key.expiresOn() + "에 " + amount + " 포인트가 만료됩니다.", "point-expiry:" + key.userId() + ":" + key.expiresOn()));
        }
        return pointLots.size();
    }

    private record ExpiryNoticeKey(Long userId, LocalDate expiresOn) {
    }
}
