package com.book.core.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.BusinessException;
import com.book.common.exception.ErrorCode;
import com.book.core.auth.application.port.RefreshSessionRepository;
import com.book.core.auth.application.usecase.AuthReissueUseCase;
import com.book.core.auth.application.usecase.CreateLoginSessionUseCase;
import com.book.core.user.application.port.UserRepositoryPort;
import com.book.core.user.application.usecase.RecoverWithdrawalUseCase;
import com.book.core.user.application.usecase.RequestUserDeletionUseCase;
import com.book.core.user.domain.User;
import io.micrometer.core.instrument.MeterRegistry;
import java.sql.DriverManager;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RedisAuthMySqlIntegrationTest {
    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4.8");
    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", MYSQL::getJdbcUrl);
        properties.add("spring.datasource.username", MYSQL::getUsername);
        properties.add("spring.datasource.password", MYSQL::getPassword);
        properties.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        properties.add("spring.sql.init.mode", () -> "never");
        properties.add("spring.data.redis.host", REDIS::getHost);
        properties.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    UserRepositoryPort users;
    @Autowired
    CreateLoginSessionUseCase login;
    @Autowired
    AuthReissueUseCase reissue;
    @Autowired
    RequestUserDeletionUseCase withdrawal;
    @Autowired
    RecoverWithdrawalUseCase recovery;
    @Autowired
    PlatformTransactionManager transactionManager;
    @Autowired
    MeterRegistry meters;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    StringRedisTemplate redis;
    @MockitoSpyBean
    RefreshSessionRepository sessions;

    @Test
    void withdrawalRevokesAndRecoveryAllowsNewSession() {
        final Long id = users.save(User.create("경합회원")).id();
        final String old = login.execute(id).refreshToken();
        withdrawal.execute(id);
        assertInvalid(old);
        recovery.execute(id);
        assertInvalid(old);
        final String fresh = login.execute(id).refreshToken();
        assertThat(reissue.execute(fresh).refreshToken()).isNotEqualTo(fresh);
    }

    @Test
    void withdrawalRollbackKeepsAccountButRevokedSessionStaysInvalid() {
        final Long id = users.save(User.create("롤백회원")).id();
        final String old = login.execute(id).refreshToken();
        new TransactionTemplate(transactionManager).executeWithoutResult((final var status) -> {
            withdrawal.execute(id);
            status.setRollbackOnly();
        });
        assertThat(users.findById(id).orElseThrow().isWithdrawalRequested()).isFalse();
        assertInvalid(old);
        assertThat(login.execute(id).refreshToken()).isNotEqualTo(old);
    }

    @Test
    void rotationRollbackInvalidatesOldTokenAndRecordsFailure() {
        final Long id = users.save(User.create("회전롤백")).id();
        final String old = login.execute(id).refreshToken();
        final var counter = meters.counter("auth.refresh_session.transaction_not_committed", "operation", "rotate");
        final var unconfirmed = meters.counter("auth.refresh_session.redis_result_unconfirmed", "operation", "rotate");
        final double before = counter.count();
        final double unconfirmedBefore = unconfirmed.count();
        new TransactionTemplate(transactionManager).executeWithoutResult((final var status) -> {
            reissue.execute(old);
            status.setRollbackOnly();
        });
        assertThat(counter.count()).isEqualTo(before + 1);
        assertThat(unconfirmed.count()).isEqualTo(unconfirmedBefore);
        assertInvalid(old);
        assertThat(login.execute(id).refreshToken()).isNotEqualTo(old);
    }

    @Test
    void failedRedisRevocationRollsBackWithdrawal() {
        final Long id = users.save(User.create("폐기실패")).id();
        final String old = login.execute(id).refreshToken();
        org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.AUTH_SESSION_UNAVAILABLE)).when(sessions).revoke(id);
        assertThatThrownBy(() -> withdrawal.execute(id)).isInstanceOfSatisfying(BusinessException.class,
            (final var exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.AUTH_SESSION_UNAVAILABLE));
        assertThat(users.findById(id).orElseThrow().isWithdrawalRequested()).isFalse();
        assertThat(reissue.execute(old).refreshToken()).isNotEqualTo(old);
    }

    @Test
    void simultaneousLoginAndWithdrawalCannotLeaveUsableSession() throws Exception {
        final Long id = users.save(User.create("동시회원")).id();
        final String old = login.execute(id).refreshToken();
        final CountDownLatch locked = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final CountDownLatch started = new CountDownLatch(1);
        final AtomicLong holderId = new AtomicLong();
        final AtomicLong waiterId = new AtomicLong();
        try (final var executor = Executors.newFixedThreadPool(2)) {
            final var deletion =
                executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult((final var status) -> {
                    holderId.set(jdbc.queryForObject("SELECT CONNECTION_ID()", Long.class));
                    withdrawal.execute(id);
                    locked.countDown();
                    awaitRelease(release);
                }));
            try {
                assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();
                final var attempt = executor.submit(() -> new TransactionTemplate(transactionManager).execute((final var status) -> {
                    waiterId.set(jdbc.queryForObject("SELECT CONNECTION_ID()", Long.class));
                    started.countDown();
                    return login.execute(id);
                }));
                assertThat(started.await(10, TimeUnit.SECONDS)).isTrue();
                assertUserLockWait(id, holderId.get(), waiterId.get());
                release.countDown();
                deletion.get(10, TimeUnit.SECONDS);
                assertThatThrownBy(() -> attempt.get(10, TimeUnit.SECONDS)).cause().isInstanceOfSatisfying(BusinessException.class,
                    (final var exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.USER_WITHDRAWAL_PENDING));
            } finally {
                release.countDown();
            }
            assertInvalid(old);
            assertThat(users.findById(id).orElseThrow().isWithdrawalRequested()).isTrue();
            assertThat(redis.hasKey("auth:refresh-session:v1:{" + id + "}")).isFalse();
        }
    }

    @Test
    void concurrentLoginsHaveExactlyOneUsableToken() throws Exception {
        final Long id = users.save(User.create("동시로그인")).id();
        final CountDownLatch start = new CountDownLatch(1);
        try (final var executor = Executors.newFixedThreadPool(2)) {
            final var first = executor.submit(() -> {
                start.await();
                return login.execute(id).refreshToken();
            });
            final var second = executor.submit(() -> {
                start.await();
                return login.execute(id).refreshToken();
            });
            start.countDown();
            final String a = first.get(20, TimeUnit.SECONDS);
            final String b = second.get(20, TimeUnit.SECONDS);
            int successes = 0;
            for (final String token : java.util.List.of(a, b)) {
                try {
                    reissue.execute(token);
                    successes++;
                } catch (final BusinessException exception) {
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
                }
            }
            assertThat(successes).isEqualTo(1);
        }
    }

    @Test
    void reissueRacingWithdrawalCannotSurviveWithdrawal() throws Exception {
        final Long id = users.save(User.create("재발급탈퇴")).id();
        final String old = login.execute(id).refreshToken();
        final CountDownLatch locked = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final CountDownLatch started = new CountDownLatch(1);
        final AtomicLong holderId = new AtomicLong();
        final AtomicLong waiterId = new AtomicLong();
        try (final var executor = Executors.newFixedThreadPool(2)) {
            final var rotation = executor.submit(() -> new TransactionTemplate(transactionManager).execute((final var status) -> {
                holderId.set(jdbc.queryForObject("SELECT CONNECTION_ID()", Long.class));
                final String token = reissue.execute(old).refreshToken();
                locked.countDown();
                awaitRelease(release);
                return token;
            }));
            try {
                assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();
                final var deletion =
                    executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult((final var status) -> {
                        waiterId.set(jdbc.queryForObject("SELECT CONNECTION_ID()", Long.class));
                        started.countDown();
                        withdrawal.execute(id);
                    }));
                assertThat(started.await(10, TimeUnit.SECONDS)).isTrue();
                assertUserLockWait(id, holderId.get(), waiterId.get());
                release.countDown();
                final String newToken = rotation.get(10, TimeUnit.SECONDS);
                deletion.get(10, TimeUnit.SECONDS);
                assertInvalid(old);
                assertInvalid(newToken);
                assertThat(users.findById(id).orElseThrow().isWithdrawalRequested()).isTrue();
                assertThat(redis.hasKey("auth:refresh-session:v1:{" + id + "}")).isFalse();
            } finally {
                release.countDown();
            }
        }
    }

    private void assertUserLockWait(final long userId, final long holderId, final long waiterId) throws Exception {
        final String sql = "SELECT COUNT(*) FROM performance_schema.data_lock_waits w "
            + "JOIN performance_schema.data_locks requested ON w.REQUESTING_ENGINE_LOCK_ID=requested.ENGINE_LOCK_ID "
            + "AND w.ENGINE=requested.ENGINE JOIN performance_schema.threads waiter ON requested.THREAD_ID=waiter.THREAD_ID "
            + "JOIN performance_schema.data_locks held ON w.BLOCKING_ENGINE_LOCK_ID=held.ENGINE_LOCK_ID AND w.ENGINE=held.ENGINE "
            + "JOIN performance_schema.threads holder ON held.THREAD_ID=holder.THREAD_ID "
            + "WHERE requested.OBJECT_SCHEMA=DATABASE() AND requested.OBJECT_NAME='users' AND requested.INDEX_NAME='PRIMARY' "
            + "AND requested.LOCK_DATA=? AND holder.PROCESSLIST_ID=? AND waiter.PROCESSLIST_ID=?";
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        try (final var connection = DriverManager.getConnection(MYSQL.getJdbcUrl(), "root", MYSQL.getPassword());
            final var statement = connection.prepareStatement(sql)) {
            statement.setString(1, Long.toString(userId));
            statement.setLong(2, holderId);
            statement.setLong(3, waiterId);
            do {
                try (final var result = statement.executeQuery()) {
                    result.next();
                    if (result.getInt(1) > 0) {
                        return;
                    }
                }
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(25));
            } while (System.nanoTime() < deadline);
        }
        throw new AssertionError("Expected MySQL user primary key lock wait between the specified connections");
    }

    private static void awaitRelease(final CountDownLatch release) {
        try {
            if (!release.await(15, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for lock release");
            }
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private void assertInvalid(final String token) {
        assertThatThrownBy(() -> reissue.execute(token)).isInstanceOfSatisfying(BusinessException.class,
            (final var exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));
    }
}
