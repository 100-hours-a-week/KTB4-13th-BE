package com.book.core.notification.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.core.notification.application.command.CreateNotificationCommand;
import com.book.core.notification.application.usecase.CreateNotificationUseCase;
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
@ActiveProfiles({"test", "mysql-test"})
@Testcontainers
@Tag("integration")
@Tag("mysql-migration")
class NotificationRepositoryIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");
    @Autowired
    CreateNotificationUseCase createUseCase;
    @Autowired
    JdbcTemplate jdbc;

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
    void 같은_이벤트_키는_한_번만_저장한다() {
        final var command = new CreateNotificationCommand(42L, "제목", "본문", "cart-low-stock:1:1:42");

        final Long firstId = createUseCase.execute(command);
        final Long secondId = createUseCase.execute(command);

        assertThat(secondId).isEqualTo(firstId);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE event_key = ?", Integer.class, command.eventKey()))
            .isEqualTo(1);
    }
}
