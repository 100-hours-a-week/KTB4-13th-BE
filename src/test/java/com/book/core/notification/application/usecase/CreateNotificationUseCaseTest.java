package com.book.core.notification.application.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.core.notification.application.command.CreateNotificationCommand;
import com.book.core.notification.application.port.NotificationRepositoryPort;
import com.book.core.notification.domain.Notification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CreateNotificationUseCaseTest {
    @Mock
    NotificationRepositoryPort repository;
    @InjectMocks
    CreateNotificationUseCase useCase;

    @Test
    void 수신자와_내용을_저장하고_발급된_ID를_반환한다() {
        final var saved = new Notification(42L, "제목", "본문");
        ReflectionTestUtils.setField(saved, "id", 100L);
        when(repository.save(any())).thenReturn(saved);
        assertThat(useCase.execute(new CreateNotificationCommand(42L, "제목", "본문"))).isEqualTo(100L);
        final var captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(42L);
        assertThat(captor.getValue().title()).isEqualTo("제목");
        assertThat(captor.getValue().body()).isEqualTo("본문");
    }

    @Test
    void 저장소_기술_오류는_전파한다() {
        when(repository.save(any())).thenThrow(new DataAccessResourceFailureException("unavailable"));
        assertThatThrownBy(() -> useCase.execute(new CreateNotificationCommand(42L, "제목", "본문")))
            .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void 이벤트_키가_있으면_중복_방지_저장을_사용한다() {
        final var saved = new Notification(42L, "제목", "본문", "cart-low-stock:1:1:42");
        ReflectionTestUtils.setField(saved, "id", 100L);
        when(repository.saveIfAbsent(any())).thenReturn(saved);

        assertThat(useCase.execute(new CreateNotificationCommand(42L, "제목", "본문", "cart-low-stock:1:1:42"))).isEqualTo(100L);

        verify(repository).saveIfAbsent(any());
    }
}
