package com.book.core.notification.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.core.notification.application.command.CreateNotificationCommand;
import com.book.core.notification.application.usecase.CreateNotificationUseCase;
import com.book.core.notification.application.port.NotificationRepositoryPort;
import com.book.core.notification.application.command.ReadNotificationCommand;
import com.book.core.notification.application.usecase.ReadNotificationUseCase;
import com.book.common.exception.CoreException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
    ReadNotificationUseCase readUseCase;
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

    @Test
    void 타인과_삭제와_없는_알림은_읽음_상태를_변경하지_못한다() {
        final Long id = createUseCase.execute(new CreateNotificationCommand(20001L, "본인 알림", "본문"));
        assertThatThrownBy(() -> readUseCase.execute(new ReadNotificationCommand(20002L, id))).isInstanceOf(CoreException.class);
        assertThat(jdbc.queryForMap("SELECT read_at FROM notifications WHERE id = ?", id)).containsEntry("read_at", null);
        assertThatThrownBy(() -> readUseCase.execute(new ReadNotificationCommand(20001L, Long.MAX_VALUE)))
            .isInstanceOf(CoreException.class);
        jdbc.update("UPDATE notifications SET deleted_at = CURRENT_TIMESTAMP(6) WHERE id = ?", id);
        assertThatThrownBy(() -> readUseCase.execute(new ReadNotificationCommand(20001L, id))).isInstanceOf(CoreException.class);
    }

    @Test
    void 동시_읽음_요청을_완료하고_반복_요청은_최초_시각을_유지한다() throws Exception {
        final Long id = createUseCase.execute(new CreateNotificationCommand(20003L, "동시 읽음", "본문"));
        final var command = new ReadNotificationCommand(20003L, id);
        final var start = new CountDownLatch(1);
        try (final var executor = Executors.newFixedThreadPool(2)) {
            final var first = executor.submit(() -> {
                start.await();
                readUseCase.execute(command);
                return null;
            });
            final var second = executor.submit(() -> {
                start.await();
                readUseCase.execute(command);
                return null;
            });
            start.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        }
        final var readAt = jdbc.queryForMap("SELECT read_at FROM notifications WHERE id = ?", id).get("read_at");
        assertThat(readAt).isNotNull();
        readUseCase.execute(command);
        assertThat(jdbc.queryForMap("SELECT read_at FROM notifications WHERE id = ?", id)).containsEntry("read_at", readAt);
        assertThat(repository.countUnreadByUserId(20003L)).isZero();
    }
}
