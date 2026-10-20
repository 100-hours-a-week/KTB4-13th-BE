package com.book.core.notification.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.core.notification.application.command.GetNotificationsCommand;
import com.book.core.notification.application.port.NotificationRepositoryPort;
import com.book.core.notification.domain.Notification;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetNotificationsUseCaseTest {
    @Mock
    NotificationRepositoryPort repository;
    @InjectMocks
    GetNotificationsUseCase useCase;

    @Test
    void 목록과_전체_미읽음_개수를_구분하여_반환한다() {
        when(repository.findRecentByUserId(42L)).thenReturn(List.of(new Notification(42L, "제목", "본문")));
        when(repository.countUnreadByUserId(42L)).thenReturn(25L);
        final var result = useCase.execute(new GetNotificationsCommand(42L));
        assertThat(result.notifications()).hasSize(1);
        assertThat(result.notifications().getFirst().body()).isEqualTo("본문");
        assertThat(result.notifications().getFirst().readAt()).isNull();
        assertThat(result.unreadCount()).isEqualTo(25L);
    }

    @Test
    void 알림이_없으면_빈_목록과_0개를_반환한다() {
        when(repository.findRecentByUserId(42L)).thenReturn(List.of());
        final var result = useCase.execute(new GetNotificationsCommand(42L));
        assertThat(result.notifications()).isEmpty();
        assertThat(result.unreadCount()).isZero();
    }

    @Test
    void 잘못된_회원_ID를_거부한다() {
        assertThatThrownBy(() -> new GetNotificationsCommand(null)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new GetNotificationsCommand(0L)).isInstanceOf(CoreException.class);
    }
}
