package com.book.core.notification.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import com.book.core.notification.application.command.CreateNotificationCommand;
import com.book.core.notification.application.command.LowStockNotificationEvent;
import com.book.core.product.application.result.ProductInventoryState;
import com.book.core.product.application.usecase.GetProductInventoryStateForUpdateUseCase;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateLowStockNotificationUseCaseTest {
    @Mock
    GetProductInventoryStateForUpdateUseCase getInventoryState;
    @Mock
    CreateNotificationUseCase createNotification;
    CreateLowStockNotificationUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateLowStockNotificationUseCase(getInventoryState, createNotification,
            Clock.fixed(Instant.parse("2026-10-26T14:59:59Z"), ZoneId.of("Asia/Seoul")));
    }

    @Test
    void 현재_재고와_버전이_유효하면_알림을_저장한다() {
        when(getInventoryState.execute(701L)).thenReturn(Optional.of(new ProductInventoryState(4, 2)));

        assertThat(useCase.execute(event(2), 7L)).isTrue();

        verify(createNotification).executeIfEventKeyAbsent(new CreateNotificationCommand(7L, "조회한 상품 품절 임박", "조회하신 '상품'의 재고가 4권 남았어요."),
            "low-stock:v3:7:701:2026-10-26");
    }

    @Test
    void 알림_저장_시각의_서울_날짜로_이벤트_키를_생성한다() {
        useCase = new CreateLowStockNotificationUseCase(getInventoryState, createNotification,
            Clock.fixed(Instant.parse("2026-10-26T15:00:00Z"), ZoneId.of("Asia/Seoul")));
        when(getInventoryState.execute(701L)).thenReturn(Optional.of(new ProductInventoryState(4, 2)));

        assertThat(useCase.execute(event(2), 7L)).isTrue();

        verify(createNotification).executeIfEventKeyAbsent(any(), org.mockito.ArgumentMatchers.eq("low-stock:v3:7:701:2026-10-27"));
    }

    @Test
    void 재고가_회복되거나_버전이_바뀌면_알림을_저장하지_않는다() {
        when(getInventoryState.execute(701L)).thenReturn(Optional.of(new ProductInventoryState(6, 2)),
            Optional.of(new ProductInventoryState(4, 3)));

        assertThat(useCase.execute(event(2), 7L)).isFalse();
        assertThat(useCase.execute(event(2), 8L)).isFalse();

        verifyNoInteractions(createNotification);
    }

    private static LowStockNotificationEvent event(final long version) {
        return new LowStockNotificationEvent(701L, 42L, "상품", version, LocalDateTime.of(2026, 10, 26, 12, 0));
    }
}
