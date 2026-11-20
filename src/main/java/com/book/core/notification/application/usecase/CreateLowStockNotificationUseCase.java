package com.book.core.notification.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.notification.application.command.CreateNotificationCommand;
import com.book.core.notification.application.command.LowStockNotificationEvent;
import com.book.core.product.application.result.ProductInventoryState;
import com.book.core.product.application.usecase.GetProductInventoryStateForUpdateUseCase;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CreateLowStockNotificationUseCase {
    private static final int LOW_STOCK_THRESHOLD = 5;

    private final GetProductInventoryStateForUpdateUseCase getProductInventoryStateForUpdateUseCase;
    private final CreateNotificationUseCase createNotificationUseCase;
    private final Clock clock;

    @Transactional
    public boolean execute(final LowStockNotificationEvent event, final Long userId) {
        final ProductInventoryState state = getProductInventoryStateForUpdateUseCase.execute(event.productId()).orElse(null);
        if (state == null || state.availableQuantity() > LOW_STOCK_THRESHOLD
            || state.lowStockNotificationVersion() != event.lowStockNotificationVersion()) {
            return false;
        }

        final boolean soldOut = state.availableQuantity() == 0;
        final String title = soldOut ? "조회한 상품 품절" : "조회한 상품 품절 임박";
        final String body = soldOut ? "조회하신 '" + event.productName() + "'의 재고가 모두 소진됐어요."
            : "조회하신 '" + event.productName() + "'의 재고가 " + state.availableQuantity() + "권 남았어요.";
        final LocalDate notificationDate = LocalDate.now(clock);
        final String eventKey = "low-stock:v3:" + userId + ":" + event.productId() + ":" + notificationDate;
        createNotificationUseCase.executeIfEventKeyAbsent(new CreateNotificationCommand(userId, title, body), eventKey);
        return true;
    }
}
