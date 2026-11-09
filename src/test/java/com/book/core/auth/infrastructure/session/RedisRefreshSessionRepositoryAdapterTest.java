package com.book.core.auth.infrastructure.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import com.book.common.exception.BusinessException;
import com.book.common.exception.ErrorCode;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@Tag("integration")
class RedisRefreshSessionRepositoryAdapterTest {
    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);
    static LettuceConnectionFactory connection;
    static StringRedisTemplate redis;
    static RedisRefreshSessionRepositoryAdapter firstAdapter;
    static RedisRefreshSessionRepositoryAdapter secondAdapter;

    @BeforeAll
    static void setUp() {
        connection = new LettuceConnectionFactory(new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(6379)));
        connection.afterPropertiesSet();
        connection.start();
        redis = new StringRedisTemplate(connection);
        firstAdapter = new RedisRefreshSessionRepositoryAdapter(redis, new SimpleMeterRegistry());
        secondAdapter = new RedisRefreshSessionRepositoryAdapter(redis, new SimpleMeterRegistry());
    }

    @AfterAll
    static void tearDown() {
        if (connection != null) {
            connection.destroy();
        }
    }

    @Test
    void concurrentRotationConsumesTokenOnce() throws Exception {
        final String originalHash = "a".repeat(64);
        final String firstNewHash = "b".repeat(64);
        final String secondNewHash = "c".repeat(64);
        final Instant expiry = Instant.now().plusSeconds(60);
        firstAdapter.replace(1001L, originalHash, expiry);
        final CountDownLatch start = new CountDownLatch(1);
        try (final var executor = Executors.newFixedThreadPool(2)) {
            final var firstRotation = executor.submit(() -> {
                start.await();
                return firstAdapter.rotate(1001L, originalHash, firstNewHash, expiry);
            });
            final var secondRotation = executor.submit(() -> {
                start.await();
                return secondAdapter.rotate(1001L, originalHash, secondNewHash, expiry);
            });
            start.countDown();
            assertThat(firstRotation.get(10, TimeUnit.SECONDS) ^ secondRotation.get(10, TimeUnit.SECONDS)).isTrue();
            assertThat(redis.opsForValue().get("auth:refresh-session:v1:{1001}")).isIn(firstNewHash, secondNewHash);
        }
    }

    @Test
    void simultaneousLoginsLeaveOneCurrentHash() throws Exception {
        final Instant expiry = Instant.now().plusSeconds(60);
        final CountDownLatch start = new CountDownLatch(1);
        try (final var executor = Executors.newFixedThreadPool(2)) {
            final var firstLogin = executor.submit(() -> {
                start.await();
                firstAdapter.replace(1007L, "a".repeat(64), expiry);
                return null;
            });
            final var secondLogin = executor.submit(() -> {
                start.await();
                secondAdapter.replace(1007L, "b".repeat(64), expiry);
                return null;
            });
            start.countDown();
            firstLogin.get(10, TimeUnit.SECONDS);
            secondLogin.get(10, TimeUnit.SECONDS);
            final String winner = redis.opsForValue().get("auth:refresh-session:v1:{1007}");
            assertThat(winner).isIn("a".repeat(64), "b".repeat(64));
            final String loser;
            if (winner.equals("a".repeat(64))) {
                loser = "b".repeat(64);
            } else {
                loser = "a".repeat(64);
            }
            assertThat(firstAdapter.rotate(1007L, loser, "c".repeat(64), expiry)).isFalse();
        }
    }

    @Test
    void lostRedisSessionRequiresLogin() {
        final Instant expiry = Instant.now().plusSeconds(60);
        firstAdapter.replace(1006L, "a".repeat(64), expiry);
        redis.delete("auth:refresh-session:v1:{1006}");
        assertThat(secondAdapter.rotate(1006L, "a".repeat(64), "b".repeat(64), expiry)).isFalse();
        secondAdapter.replace(1006L, "c".repeat(64), expiry);
        assertThat(redis.opsForValue().get("auth:refresh-session:v1:{1006}")).isEqualTo("c".repeat(64));
    }

    @Test
    void authSessionOperationsDoNotChangeCouponKeys() {
        final String couponKey = "coupon:1012:stock";
        redis.opsForValue().set(couponKey, "17");
        final Instant expiry = Instant.now().plusSeconds(60);
        firstAdapter.replace(1012L, "a".repeat(64), expiry);
        assertThat(secondAdapter.rotate(1012L, "a".repeat(64), "b".repeat(64), expiry)).isTrue();
        firstAdapter.revoke(1012L);
        assertThat(redis.opsForValue().get(couponKey)).isEqualTo("17");
    }

    @Test
    void sessionReplacementAndRevocationAreSharedAndIsolatedByUser() {
        final Instant expiry = Instant.now().plusSeconds(60);
        firstAdapter.replace(1009L, "a".repeat(64), expiry);
        assertThat(secondAdapter.rotate(1010L, "a".repeat(64), "b".repeat(64), expiry)).isFalse();
        secondAdapter.revoke(1010L);
        assertThat(redis.opsForValue().get("auth:refresh-session:v1:{1009}")).isEqualTo("a".repeat(64));
        secondAdapter.replace(1009L, "b".repeat(64), expiry);
        assertThat(firstAdapter.rotate(1009L, "a".repeat(64), "c".repeat(64), expiry)).isFalse();
        firstAdapter.revoke(1009L);
        assertThat(secondAdapter.rotate(1009L, "b".repeat(64), "c".repeat(64), expiry)).isFalse();
    }

    @Test
    void expiresAtTheJwtExpiryTime() {
        firstAdapter.replace(1004L, "d".repeat(64), Instant.now().plusMillis(500));
        org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(5))
            .untilAsserted(() -> assertThat(redis.hasKey("auth:refresh-session:v1:{1004}")).isFalse());
        assertThat(firstAdapter.rotate(1004L, "d".repeat(64), "e".repeat(64), Instant.now().plusSeconds(10))).isFalse();
    }

    @Test
    void concurrentRotationAndLogoutLeaveNoUsableSession() throws Exception {
        final Instant expiry = Instant.now().plusSeconds(60);
        firstAdapter.replace(1005L, "a".repeat(64), expiry);
        final CountDownLatch start = new CountDownLatch(1);
        try (final var executor = Executors.newFixedThreadPool(2)) {
            final var rotation = executor.submit(() -> {
                start.await();
                return firstAdapter.rotate(1005L, "a".repeat(64), "b".repeat(64), expiry);
            });
            final var logout = executor.submit(() -> {
                start.await();
                secondAdapter.revoke(1005L);
                return null;
            });
            start.countDown();
            rotation.get(10, TimeUnit.SECONDS);
            logout.get(10, TimeUnit.SECONDS);
            assertThat(redis.hasKey("auth:refresh-session:v1:{1005}")).isFalse();
        }
    }

    @Test
    void concurrentLoginAndRotationLeaveTheLoginSession() throws Exception {
        final Instant expiry = Instant.now().plusSeconds(60);
        firstAdapter.replace(1011L, "a".repeat(64), expiry);
        final CountDownLatch start = new CountDownLatch(1);
        try (final var executor = Executors.newFixedThreadPool(2)) {
            final var login = executor.submit(() -> {
                start.await();
                firstAdapter.replace(1011L, "b".repeat(64), expiry);
                return null;
            });
            final var rotation = executor.submit(() -> {
                start.await();
                return secondAdapter.rotate(1011L, "a".repeat(64), "c".repeat(64), expiry);
            });
            start.countDown();
            login.get(10, TimeUnit.SECONDS);
            rotation.get(10, TimeUnit.SECONDS);
            assertThat(redis.opsForValue().get("auth:refresh-session:v1:{1011}")).isEqualTo("b".repeat(64));
        }
    }

    @Test
    void redisFailureIsServiceUnavailable() {
        final LettuceConnectionFactory unavailable = new LettuceConnectionFactory(new RedisStandaloneConfiguration("127.0.0.1", 1));
        unavailable.afterPropertiesSet();
        unavailable.start();
        try {
            final var adapter = new RedisRefreshSessionRepositoryAdapter(new StringRedisTemplate(unavailable), new SimpleMeterRegistry());
            assertThatThrownBy(() -> adapter.revoke(1003L)).isInstanceOfSatisfying(BusinessException.class,
                (final var exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.AUTH_SESSION_UNAVAILABLE));
        } finally {
            unavailable.destroy();
        }

        final String key = "auth:refresh-session:v1:{1003}";
        firstAdapter.replace(1003L, "a".repeat(64), Instant.now().plusSeconds(60));
        final StringRedisTemplate uncertainRedis = spy(redis);
        doAnswer((final var invocation) -> {
            invocation.callRealMethod();
            throw new RedisConnectionFailureException("simulated lost response");
        }).when(uncertainRedis).delete(key);
        final var meters = new SimpleMeterRegistry();
        final var adapter = new RedisRefreshSessionRepositoryAdapter(uncertainRedis, meters);
        assertThatThrownBy(() -> adapter.revoke(1003L)).isInstanceOfSatisfying(BusinessException.class,
            (final var exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.AUTH_SESSION_UNAVAILABLE));
        assertThat(redis.hasKey(key)).isFalse();
        assertThat(meters.counter("auth.refresh_session.redis_result_unconfirmed", "operation", "revoke").count()).isEqualTo(1);
        assertThat(meters.find("auth.refresh_session.transaction_not_committed").counter()).isNull();
    }
}
