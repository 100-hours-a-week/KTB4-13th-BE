package com.book.core.auth.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

import com.book.common.exception.BusinessException;
import com.book.common.exception.ErrorCode;
import com.book.core.auth.application.port.RefreshTokenHasher;
import com.book.core.auth.application.result.AuthLoginResult;
import com.book.core.user.application.port.UserRepositoryPort;
import com.book.core.user.application.usecase.RecoverWithdrawalUseCase;
import com.book.core.user.application.usecase.RequestUserDeletionUseCase;
import com.book.core.user.domain.User;
import java.sql.DriverManager;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles({"test", "mysql-test"})
@Testcontainers
@Tag("integration")
@Tag("mysql-migration")
class AuthSessionMySqlConcurrencyTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");
    @Container
    static final GenericContainer<?> redisContainer = new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(final DynamicPropertyRegistry properties) {
        properties.add("spring.data.redis.host", redisContainer::getHost);
        properties.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
    }

    @Autowired
    CreateLoginSessionUseCase login;
    @Autowired
    AuthReissueUseCase reissue;
    @Autowired
    AuthLogoutUseCase logout;
    @Autowired
    RequestUserDeletionUseCase withdrawal;
    @Autowired
    RecoverWithdrawalUseCase recover;
    @Autowired
    UserRepositoryPort users;
    @MockitoSpyBean
    RefreshTokenHasher hasher;
    @Autowired
    StringRedisTemplate redis;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    PlatformTransactionManager manager;

    @Test
    void loginWaitsForUserLockAndReadsCommittedSession() throws Exception {
        final long userId = users.save(User.create("동시로그인회원")).id();
        assertThat(jdbc.queryForObject("SELECT @@transaction_isolation", String.class)).isEqualTo("REPEATABLE-READ");
        final CountDownLatch written = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final CountDownLatch started = new CountDownLatch(1);
        final AtomicLong holderConnection = new AtomicLong();
        final AtomicLong waiterConnection = new AtomicLong();
        try (final var executor = Executors.newFixedThreadPool(2)) {
            final Future<AuthLoginResult> first = executor.submit(() -> new TransactionTemplate(manager).execute((final var status) -> {
                holderConnection.set(connectionId());
                final AuthLoginResult result = login.execute(userId);
                written.countDown();
                await(release);
                return result;
            }));
            try {
                assertThat(written.await(10, TimeUnit.SECONDS)).isTrue();
                final Future<AuthLoginResult> second =
                    executor.submit(inTransaction(waiterConnection, started, () -> login.execute(userId)));
                assertThat(started.await(10, TimeUnit.SECONDS)).isTrue();
                assertUserLockWait(userId, holderConnection.get(), waiterConnection.get());
                release.countDown();
                final AuthLoginResult firstResult = first.get(10, TimeUnit.SECONDS);
                final AuthLoginResult secondResult = second.get(10, TimeUnit.SECONDS);
                assertThat(firstResult.refreshToken()).isNotEqualTo(secondResult.refreshToken());
                assertThat(activeCount(userId)).isEqualTo(1);
                assertThat(redis.opsForValue().get(sessionKey(userId))).isEqualTo(hasher.hash(secondResult.refreshToken()));
            } finally {
                release.countDown();
            }
        }
    }

    @Test
    void concurrentRotationAndLogoutWaitForCommittedSuccessor() throws Exception {
        final long userId = users.save(User.create("경합회전회원")).id();
        final String token = login.execute(userId).refreshToken();
        final CountDownLatch rotated = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final CountDownLatch started = new CountDownLatch(2);
        final AtomicLong holderConnection = new AtomicLong();
        final AtomicLong duplicateConnection = new AtomicLong();
        final AtomicLong logoutConnection = new AtomicLong();
        try (final var executor = Executors.newFixedThreadPool(3)) {
            final Future<?> first = executor.submit(() -> new TransactionTemplate(manager).executeWithoutResult((final var status) -> {
                holderConnection.set(connectionId());
                reissue.execute(token);
                rotated.countDown();
                await(release);
            }));
            try {
                assertThat(rotated.await(10, TimeUnit.SECONDS)).isTrue();
                final Future<?> duplicate = executor.submit(inTransaction(duplicateConnection, started, () -> reissue.execute(token)));
                final Future<?> end = executor.submit(inTransaction(logoutConnection, started, () -> {
                    logout.execute(userId);
                    return null;
                }));
                assertThat(started.await(10, TimeUnit.SECONDS)).isTrue();
                assertUserLockWait(userId, holderConnection.get(), duplicateConnection.get());
                assertUserLockWait(userId, holderConnection.get(), logoutConnection.get());
                release.countDown();
                first.get(10, TimeUnit.SECONDS);
                assertThatThrownBy(() -> duplicate.get(10, TimeUnit.SECONDS)).cause().isInstanceOfSatisfying(BusinessException.class,
                    (final var exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));
                end.get(10, TimeUnit.SECONDS);
                assertThat(activeCount(userId)).isZero();
            } finally {
                release.countDown();
            }
        }
    }

    @Test
    void withdrawalBlocksLoginAndRecoveryAllowsLogin() throws Exception {
        final long userId = users.save(User.create("탈퇴경합회원")).id();
        login.execute(userId);
        final CountDownLatch withdrawn = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final CountDownLatch started = new CountDownLatch(1);
        final AtomicLong holderConnection = new AtomicLong();
        final AtomicLong waiterConnection = new AtomicLong();
        try (final var executor = Executors.newFixedThreadPool(2)) {
            final Future<?> first = executor.submit(() -> new TransactionTemplate(manager).executeWithoutResult((final var status) -> {
                holderConnection.set(connectionId());
                withdrawal.execute(userId);
                withdrawn.countDown();
                await(release);
            }));
            try {
                assertThat(withdrawn.await(10, TimeUnit.SECONDS)).isTrue();
                final Future<?> second = executor.submit(inTransaction(waiterConnection, started, () -> login.execute(userId)));
                assertThat(started.await(10, TimeUnit.SECONDS)).isTrue();
                assertUserLockWait(userId, holderConnection.get(), waiterConnection.get());
                release.countDown();
                first.get(10, TimeUnit.SECONDS);
                assertThatThrownBy(() -> second.get(10, TimeUnit.SECONDS)).cause().isInstanceOfSatisfying(BusinessException.class,
                    (final var exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.USER_WITHDRAWAL_PENDING));
                assertThat(activeCount(userId)).isZero();
                recover.execute(userId);
                login.execute(userId);
                assertThat(activeCount(userId)).isEqualTo(1);
            } finally {
                release.countDown();
            }
        }
    }

    @Test
    void failedOuterTransactionLeavesRedisReplacementInPlace() {
        final long userId = users.save(User.create("롤백검증회원")).id();
        final String original = login.execute(userId).refreshToken();
        assertThatThrownBy(() -> new TransactionTemplate(manager).executeWithoutResult((final var status) -> {
            login.execute(userId);
            throw new IllegalStateException("rollback");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(redis.opsForValue().get(sessionKey(userId))).isNotEqualTo(hasher.hash(original));
        assertThatThrownBy(() -> reissue.execute(original)).isInstanceOfSatisfying(BusinessException.class,
            (final var error) -> assertThat(error.errorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));
    }

    @Test
    void invalidRedisHashDoesNotReplacePreviousSession() {
        final long userId = users.save(User.create("저장실패회원")).id();
        final String oldToken = login.execute(userId).refreshToken();
        final String oldHash = hasher.hash(oldToken);
        doReturn("x".repeat(65)).when(hasher).hash(anyString());

        assertThatThrownBy(() -> login.execute(userId)).isInstanceOfSatisfying(BusinessException.class,
            (final var error) -> assertThat(error.errorCode()).isEqualTo(ErrorCode.TOKEN_ISSUE_FAILURE));
        assertThat(redis.opsForValue().get(sessionKey(userId))).isEqualTo(oldHash);
    }

    private <T> Callable<T> inTransaction(final AtomicLong connection, final CountDownLatch started, final Supplier<T> operation) {
        return () -> new TransactionTemplate(manager).execute((final var status) -> {
            connection.set(connectionId());
            started.countDown();
            return operation.get();
        });
    }

    private long connectionId() {
        return jdbc.queryForObject("SELECT CONNECTION_ID()", Long.class);
    }

    private void assertUserLockWait(final long userId, final long holderConnection, final long waiterConnection) throws Exception {
        final String sql = "SELECT COUNT(*) FROM performance_schema.data_lock_waits w "
            + "JOIN performance_schema.data_locks requested ON w.REQUESTING_ENGINE_LOCK_ID=requested.ENGINE_LOCK_ID "
            + "AND w.ENGINE=requested.ENGINE JOIN performance_schema.threads waiter ON requested.THREAD_ID=waiter.THREAD_ID "
            + "JOIN performance_schema.data_locks held ON w.BLOCKING_ENGINE_LOCK_ID=held.ENGINE_LOCK_ID AND w.ENGINE=held.ENGINE "
            + "JOIN performance_schema.threads holder ON held.THREAD_ID=holder.THREAD_ID "
            + "WHERE requested.OBJECT_SCHEMA=DATABASE() AND requested.OBJECT_NAME='users' AND requested.INDEX_NAME='PRIMARY' "
            + "AND requested.LOCK_DATA=? AND holder.PROCESSLIST_ID=? AND waiter.PROCESSLIST_ID=?";
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        try (final var connection = DriverManager.getConnection(mysql.getJdbcUrl(), "root", mysql.getPassword());
            final var statement = connection.prepareStatement(sql)) {
            statement.setString(1, Long.toString(userId));
            statement.setLong(2, holderConnection);
            statement.setLong(3, waiterConnection);
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
        throw new AssertionError("Expected user PK lock wait for the specified MySQL connections and user");
    }

    private String sessionKey(final long userId) {
        return "auth:refresh-session:v1:{" + userId + "}";
    }

    private int activeCount(final long userId) {
        if (Boolean.TRUE.equals(redis.hasKey(sessionKey(userId)))) {
            return 1;
        }
        return 0;
    }

    private static void await(final CountDownLatch latch) {
        try {
            if (!latch.await(15, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timed out");
            }
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
