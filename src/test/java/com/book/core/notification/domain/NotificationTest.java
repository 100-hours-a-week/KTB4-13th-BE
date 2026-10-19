package com.book.core.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.core.notification.application.command.CreateNotificationCommand;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

class NotificationTest {
    @Test
    void 신규_알림은_수신자와_내용을_보존하며_읽지_않은_상태다() {
        final var notification = new Notification(42L, "알림", "새 소식");
        assertThat(notification.id()).isNull();
        assertThat(notification.userId()).isEqualTo(42L);
        assertThat(notification.title()).isEqualTo("알림");
        assertThat(notification.body()).isEqualTo("새 소식");
        assertThat(notification.readAt()).isNull();
    }

    @Test
    void 잘못된_수신자와_빈_내용과_길이_초과를_거부한다() {
        assertThatThrownBy(() -> new Notification(null, "제목", "본문")).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new Notification(0L, "제목", "본문")).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new Notification(1L, "  ", "본문")).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new Notification(1L, "제목", null)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new Notification(1L, "가".repeat(101), "본문")).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new Notification(1L, "제목", "가".repeat(1001))).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new CreateNotificationCommand(-1L, "제목", "본문")).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new CreateNotificationCommand(1L, null, "본문")).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new CreateNotificationCommand(1L, "제목", " ")).isInstanceOf(CoreException.class);
    }

    @Test
    void 최대_길이의_내용을_허용한다() {
        assertThat(new Notification(1L, "가".repeat(100), "나".repeat(1000)).body()).hasSize(1000);
    }

    @Test
    void 반복_읽음은_최초_시각을_유지한다() {
        final var notification = new Notification(42L, "제목", "본문");
        final var firstReadAt = LocalDateTime.of(2026, 10, 19, 10, 0);
        notification.markRead(firstReadAt);
        notification.markRead(firstReadAt.plusHours(1));
        assertThat(notification.readAt()).isEqualTo(firstReadAt);
        assertThatThrownBy(() -> notification.markRead(null)).isInstanceOf(CoreException.class);
    }
}
