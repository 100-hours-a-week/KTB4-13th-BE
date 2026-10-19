package com.book.core.notification.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.core.notification.application.command.CreateNotificationCommand;
import com.book.core.notification.application.usecase.CreateNotificationUseCase;
import com.book.core.notification.application.port.NotificationRepositoryPort;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
class NotificationRepositoryIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");
    @Autowired
    CreateNotificationUseCase createUseCase;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    NotificationRepositoryPort repository;

    @Test
    void 신규_알림과_공통_생명주기를_MySQL에_저장한다() {
        final Long id = createUseCase.execute(new CreateNotificationCommand(42L, "제목", "본문"));
        final var row = jdbc.queryForMap("SELECT * FROM notifications WHERE id = ?", id);
        assertThat(row).containsEntry("user_id", 42L).containsEntry("title", "제목").containsEntry("body", "본문")
            .containsEntry("read_at", null).containsEntry("deleted_at", null);
        assertThat(row.get("created_at")).isNotNull();
        assertThat(row.get("updated_at")).isNotNull();
    }

    @Test
    void DB가_잘못된_수신자와_빈_제목을_차단한다() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO notifications (user_id, title, body) VALUES (0, '제목', '본문')"))
            .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO notifications (user_id, title, body) VALUES (1, ' ', '본문')"))
            .isInstanceOf(DataAccessException.class);
    }

    @Test
    void 최근_20개를_ID_역순으로_조회하고_전체_미읽음을_집계한다() {
        final Long userId = 10001L;
        for (int i = 0; i < 25; i++) {
            createUseCase.execute(new CreateNotificationCommand(userId, "제목 " + i, "본문"));
        }
        createUseCase.execute(new CreateNotificationCommand(10002L, "타인 알림", "본문"));
        final Long deletedId = createUseCase.execute(new CreateNotificationCommand(userId, "삭제 알림", "본문"));
        jdbc.update("UPDATE notifications SET deleted_at = CURRENT_TIMESTAMP(6) WHERE id = ?", deletedId);
        final var items = repository.findRecentByUserId(userId);
        assertThat(items).hasSize(20);
        assertThat(items).allSatisfy((final var item) -> assertThat(item.userId()).isEqualTo(userId));
        assertThat(items).extracting((final var item) -> item.id()).isSortedAccordingTo(java.util.Comparator.reverseOrder());
        assertThat(items.getFirst().title()).isEqualTo("제목 24");
        assertThat(repository.countUnreadByUserId(userId)).isEqualTo(25L);
        jdbc.update("UPDATE notifications SET read_at = CURRENT_TIMESTAMP(6) WHERE id = ?", items.getFirst().id());
        assertThat(repository.countUnreadByUserId(userId)).isEqualTo(24L);
        assertThat(repository.findRecentByUserId(10003L)).isEmpty();
    }
}
