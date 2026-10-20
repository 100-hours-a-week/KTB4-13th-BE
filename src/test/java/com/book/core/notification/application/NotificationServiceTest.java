package com.book.core.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.core.notification.application.command.CreateNotificationCommand;
import com.book.core.notification.application.service.NotificationService;
import com.book.core.notification.application.usecase.CreateNotificationUseCase;
import com.book.core.notification.application.command.GetNotificationsCommand;
import com.book.core.notification.application.result.GetNotificationsResult;
import java.util.List;
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
    void 목록_조회를_UseCase에_위임한다() {
        final var command = new GetNotificationsCommand(42L);
        final var result = new GetNotificationsResult(List.of(), 0L);
        when(getUseCase.execute(command)).thenReturn(result);
        assertThat(service.getRecent(command)).isSameAs(result);
        verify(getUseCase).execute(command);
    }
}
