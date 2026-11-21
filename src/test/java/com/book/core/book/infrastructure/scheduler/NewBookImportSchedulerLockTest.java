package com.book.core.book.infrastructure.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.book.common.config.scheduler.SchedulerLockConfig;
import com.book.core.book.application.service.NewBookImportService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class NewBookImportSchedulerLockTest {
    private final NewBookImportService imports = mock(NewBookImportService.class);
    private final MeterRegistry registry = new SimpleMeterRegistry();
    private AnnotationConfigApplicationContext context;

    @AfterEach
    void closeContext() {
        context.close();
    }

    @Test
    void 다른_인스턴스가_실행_중이면_배치를_건너뛰고_건너뜀을_기록한다() throws Exception {
        final var scheduler = start("PT0S");
        final var started = new CountDownLatch(1);
        final var release = new CountDownLatch(1);
        doAnswer((final var invocation) -> {
            started.countDown();
            assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
            return null;
        }).when(imports).importNewBooks(any());

        try (var executor = Executors.newFixedThreadPool(2)) {
            final var first = executor.submit(scheduler::importNewBooks);
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
            executor.submit(scheduler::importNewBooks).get(5, TimeUnit.SECONDS);
            release.countDown();
            first.get(5, TimeUnit.SECONDS);
        }

        verify(imports, times(1)).importNewBooks(any());
        assertThat(attempts("acquired")).isEqualTo(1);
        assertThat(attempts("not_acquired")).isEqualTo(1);
    }

    @Test
    void 최소_점유_시간_안에는_끝난_뒤에도_다시_실행하지_않는다() throws Exception {
        final var scheduler = start("PT5M");

        try (var executor = Executors.newSingleThreadExecutor()) {
            executor.submit(scheduler::importNewBooks).get(5, TimeUnit.SECONDS);
        }
        try (var executor = Executors.newSingleThreadExecutor()) {
            executor.submit(scheduler::importNewBooks).get(5, TimeUnit.SECONDS);
        }

        verify(imports, times(1)).importNewBooks(any());
        assertThat(attempts("not_acquired")).isEqualTo(1);
    }

    private NewBookImportScheduler start(final String lockAtLeastFor) {
        final var jdbc =
            new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1"));
        jdbc.execute("""
            CREATE TABLE shedlock (name VARCHAR(64) NOT NULL PRIMARY KEY, lock_until TIMESTAMP(3) NOT NULL,
                locked_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), locked_by VARCHAR(255) NOT NULL)
            """);
        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", Map.of(
            "app.new-book.enabled", "true",
            "app.new-book.national-library-key", "test",
            "app.new-book.data-for-library-key", "test",
            "app.new-book.lock-at-least-for", lockAtLeastFor)));
        context.registerBean(JdbcTemplate.class, () -> jdbc);
        context.registerBean(MeterRegistry.class, () -> registry);
        context.registerBean(NewBookImportService.class, () -> imports);
        context.register(SchedulerLockConfig.class, NewBookImportScheduler.class);
        context.refresh();
        return context.getBean(NewBookImportScheduler.class);
    }

    private double attempts(final String result) {
        return registry.get("scheduler.lock.attempts").tag("lock_name", "new-book-import").tag("result", result).counter().count();
    }
}
