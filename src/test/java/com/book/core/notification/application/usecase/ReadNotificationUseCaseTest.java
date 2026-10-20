package com.book.core.notification.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.notification.application.command.ReadNotificationCommand;
import com.book.core.notification.application.port.NotificationRepositoryPort;
import com.book.core.notification.domain.Notification;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReadNotificationUseCaseTest {
    @Mock
    NotificationRepositoryPort repository;
    @InjectMocks
    ReadNotificationUseCase useCase;

    @Test
    void 소유자_조건으로_잠금_조회한_알림을_읽음_처리한다() {
        final var notification = new Notification(42L, "제목", "본문");
        when(repository.findOwnedActiveForUpdate(42L, 10L)).thenReturn(Optional.of(notification));
        useCase.execute(new ReadNotificationCommand(42L, 10L));
        assertThat(notification.readAt()).isNotNull();
        verify(repository).save(notification);
        final var firstReadAt = notification.readAt();
        useCase.execute(new ReadNotificationCommand(42L, 10L));
        assertThat(notification.readAt()).isEqualTo(firstReadAt);
    }

    @Test
    void 본인_활성_알림이_아니면_동일한_404_오류를_사용한다() {
        when(repository.findOwnedActiveForUpdate(42L, 10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.execute(new ReadNotificationCommand(42L, 10L))).isInstanceOfSatisfying(CoreException.class,
            (final var error) -> assertThat(error.errorCode()).isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND));
        verify(repository).findOwnedActiveForUpdate(42L, 10L);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void 잘못된_회원과_알림_ID를_거부한다() {
        assertThatThrownBy(() -> new ReadNotificationCommand(null, 1L)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new ReadNotificationCommand(1L, null)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new ReadNotificationCommand(1L, 0L)).isInstanceOf(CoreException.class);
    }
}
