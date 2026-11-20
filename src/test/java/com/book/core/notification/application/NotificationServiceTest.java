package com.book.core.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

import com.book.core.notification.application.command.CreateNotificationCommand;
import com.book.core.notification.application.command.LowStockNotificationEvent;
import com.book.core.notification.application.service.NotificationService;
import com.book.core.notification.application.usecase.CreateNotificationUseCase;
import com.book.core.notification.application.usecase.CreateLowStockNotificationUseCase;
import com.book.core.product.application.usecase.FindLowStockNotificationCandidatesUseCase;
import java.time.LocalDateTime;
import com.book.core.notification.application.command.GetNotificationsCommand;
import com.book.core.notification.application.result.GetNotificationsResult;
import java.util.List;
import com.book.core.notification.application.command.ReadNotificationCommand;
import com.book.core.notification.application.usecase.ReadNotificationUseCase;
import com.book.core.notification.application.usecase.GetNotificationsUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock
    CreateNotificationUseCase createUseCase;
    @Mock
    GetNotificationsUseCase getUseCase;
    @Mock
    ReadNotificationUseCase readUseCase;
    @Mock
    FindLowStockNotificationCandidatesUseCase findCandidatesUseCase;
    @Mock
    CreateLowStockNotificationUseCase createLowStockNotificationUseCase;
    @InjectMocks
    NotificationService service;

    @Test
    void 알림_생성을_UseCase에_위임한다() {
        final var command = new CreateNotificationCommand(42L, "제목", "본문");
        when(createUseCase.execute(command)).thenReturn(10L);
        assertThat(service.create(command)).isEqualTo(10L);
        verify(createUseCase).execute(command);
    }

    @Test
    void 일괄_알림_저장은_한_건이_실패해도_다음_알림을_저장한다() {
        final var first = new CreateNotificationCommand(42L, "제목", "본문");
        final var second = new CreateNotificationCommand(43L, "제목", "본문");
        when(createUseCase.execute(first)).thenThrow(new IllegalStateException("저장 실패"));

        assertThatCode(() -> service.createAllAsync(List.of(first, second))).doesNotThrowAnyException();

        verify(createUseCase).execute(second);
    }

    @Test
    void V3_후보가_없으면_알림을_저장하지_않는다() {
        final var event = event();
        when(findCandidatesUseCase.execute(event.productId(), event.referenceTime())).thenReturn(List.of());

        service.createLowStockNotificationsAsync(event);

        verifyNoInteractions(createLowStockNotificationUseCase);
    }

    @Test
    void V3는_주문자를_제외하고_건별_실패를_격리한다() {
        final var event = event();
        when(findCandidatesUseCase.execute(event.productId(), event.referenceTime())).thenReturn(List.of(42L, 7L, 8L));
        when(createLowStockNotificationUseCase.execute(event, 7L)).thenThrow(new IllegalStateException("저장 실패"));
        when(createLowStockNotificationUseCase.execute(event, 8L)).thenReturn(true);

        service.createLowStockNotificationsAsync(event);

        verify(createLowStockNotificationUseCase).execute(event, 7L);
        verify(createLowStockNotificationUseCase).execute(event, 8L);
    }

    @Test
    void V3_이벤트가_무효하면_나머지_수신자를_처리하지_않는다() {
        final var event = event();
        when(findCandidatesUseCase.execute(event.productId(), event.referenceTime())).thenReturn(List.of(7L, 8L));
        when(createLowStockNotificationUseCase.execute(event, 7L)).thenReturn(false);

        service.createLowStockNotificationsAsync(event);

        verify(createLowStockNotificationUseCase).execute(event, 7L);
        org.mockito.Mockito.verifyNoMoreInteractions(createLowStockNotificationUseCase);
    }

    private static LowStockNotificationEvent event() {
        return new LowStockNotificationEvent(701L, 42L, "상품", 1, LocalDateTime.of(2026, 10, 26, 12, 0));
    }

    @Test
    void 목록_조회를_UseCase에_위임한다() {
        final var command = new GetNotificationsCommand(42L);
        final var result = new GetNotificationsResult(List.of(), 0L);
        when(getUseCase.execute(command)).thenReturn(result);
        assertThat(service.getRecent(command)).isSameAs(result);
        verify(getUseCase).execute(command);
    }

    @Test
    void 읽음_처리를_UseCase에_위임한다() {
        final var command = new ReadNotificationCommand(42L, 10L);
        service.read(command);
        verify(readUseCase).execute(command);
    }
}
