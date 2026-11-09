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
    static RedisRefreshSessionRepositoryAdapter first;
    static RedisRefreshSessionRepositoryAdapter second;

    @BeforeAll
    static void setUp() {
        connection = new LettuceConnectionFactory(new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(6379)));
        connection.afterPropertiesSet();
        connection.start();
        redis = new StringRedisTemplate(connection);
        first = new RedisRefreshSessionRepositoryAdapter(redis, new SimpleMeterRegistry());
        second = new RedisRefreshSessionRepositoryAdapter(redis, new SimpleMeterRegistry());
    }

    @AfterAll
    static void tearDown() {
        if (connection != null) {
            connection.destroy();
        }
    }

    @Test
    void concurrentRotationConsumesTokenOnce() throws Exception {
        final String original = "a".repeat(64);
        final String nextA = "b".repeat(64);
        final String nextB = "c".repeat(64);
        final Instant expiry = Instant.now().plusSeconds(60);
        first.replace(1001L, original, expiry);
        final CountDownLatch start = new CountDownLatch(1);
        try (final var executor = Executors.newFixedThreadPool(2)) {
            final var a = executor.submit(() -> {
                start.await();
                return first.rotate(1001L, original, nextA, expiry);
            });
            final var b = executor.submit(() -> {
                start.await();
                return second.rotate(1001L, original, nextB, expiry);
            });
            start.countDown();
            assertThat(a.get(10, TimeUnit.SECONDS) ^ b.get(10, TimeUnit.SECONDS)).isTrue();
            assertThat(redis.opsForValue().get("auth:refresh-session:v1:{1001}")).isIn(nextA, nextB);
        }
    }

    @Test
    void simultaneousLoginsLeaveOneCurrentHash() throws Exception {
        final Instant expiry = Instant.now().plusSeconds(60);
        final CountDownLatch start = new CountDownLatch(1);
        try (final var executor = Executors.newFixedThreadPool(2)) {
            final var a = executor.submit(() -> {
                start.await();
                first.replace(1007L, "a".repeat(64), expiry);
                return null;
            });
            final var b = executor.submit(() -> {
                start.await();
                second.replace(1007L, "b".repeat(64), expiry);
                return null;
            });
            start.countDown();
            a.get(10, TimeUnit.SECONDS);
            b.get(10, TimeUnit.SECONDS);
            final String winner = redis.opsForValue().get("auth:refresh-session:v1:{1007}");
            assertThat(winner).isIn("a".repeat(64), "b".repeat(64));
            final String loser;
            if (winner.equals("a".repeat(64))) {
                loser = "b".repeat(64);
            } else {
                loser = "a".repeat(64);
            }
            assertThat(first.rotate(1007L, loser, "c".repeat(64), expiry)).isFalse();
        }
    }

    @Test
    void lostRedisSessionRequiresLogin() {
        final Instant expiry = Instant.now().plusSeconds(60);
        first.replace(1006L, "a".repeat(64), expiry);
        redis.delete("auth:refresh-session:v1:{1006}");
        assertThat(second.rotate(1006L, "a".repeat(64), "b".repeat(64), expiry)).isFalse();
        second.replace(1006L, "c".repeat(64), expiry);
        assertThat(redis.opsForValue().get("auth:refresh-session:v1:{1006}")).isEqualTo("c".repeat(64));
    }

    @Test
    void authSessionOperationsDoNotChangeCouponKeys() {
        final String couponKey = "coupon:1012:stock";
        redis.opsForValue().set(couponKey, "17");
        final Instant expiry = Instant.now().plusSeconds(60);
        first.replace(1012L, "a".repeat(64), expiry);
        assertThat(second.rotate(1012L, "a".repeat(64), "b".repeat(64), expiry)).isTrue();
        first.revoke(1012L);
        assertThat(redis.opsForValue().get(couponKey)).isEqualTo("17");
    }

    @Test
    void sessionReplacementAndRevocationAreSharedAndIsolatedByUser() {
        final Instant expiry = Instant.now().plusSeconds(60);
        first.replace(1009L, "a".repeat(64), expiry);
        assertThat(second.rotate(1010L, "a".repeat(64), "b".repeat(64), expiry)).isFalse();
        second.revoke(1010L);
        assertThat(redis.opsForValue().get("auth:refresh-session:v1:{1009}")).isEqualTo("a".repeat(64));
        second.replace(1009L, "b".repeat(64), expiry);
        assertThat(first.rotate(1009L, "a".repeat(64), "c".repeat(64), expiry)).isFalse();
        first.revoke(1009L);
        assertThat(second.rotate(1009L, "b".repeat(64), "c".repeat(64), expiry)).isFalse();
    }

    @Test
    void expiresAtTheJwtExpiryTime() {
        first.replace(1004L, "d".repeat(64), Instant.now().plusMillis(500));
        org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(5))
            .untilAsserted(() -> assertThat(redis.hasKey("auth:refresh-session:v1:{1004}")).isFalse());
        assertThat(first.rotate(1004L, "d".repeat(64), "e".repeat(64), Instant.now().plusSeconds(10))).isFalse();
    }

    @Test
    void concurrentRotationAndLogoutLeaveNoUsableSession() throws Exception {
        final Instant expiry = Instant.now().plusSeconds(60);
        first.replace(1005L, "a".repeat(64), expiry);
        final CountDownLatch start = new CountDownLatch(1);
        try (final var executor = Executors.newFixedThreadPool(2)) {
            final var rotation = executor.submit(() -> {
                start.await();
                return first.rotate(1005L, "a".repeat(64), "b".repeat(64), expiry);
            });
            final var logout = executor.submit(() -> {
                start.await();
                second.revoke(1005L);
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
        first.replace(1011L, "a".repeat(64), expiry);
        final CountDownLatch start = new CountDownLatch(1);
        try (final var executor = Executors.newFixedThreadPool(2)) {
            final var login = executor.submit(() -> {
                start.await();
                first.replace(1011L, "b".repeat(64), expiry);
                return null;
            });
            final var rotation = executor.submit(() -> {
                start.await();
                return second.rotate(1011L, "a".repeat(64), "c".repeat(64), expiry);
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
        first.replace(1003L, "a".repeat(64), Instant.now().plusSeconds(60));
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
