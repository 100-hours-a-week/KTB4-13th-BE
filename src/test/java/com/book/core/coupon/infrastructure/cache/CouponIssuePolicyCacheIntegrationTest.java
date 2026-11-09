package com.book.core.coupon.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.coupon.application.service.CacheMetrics;
import com.book.core.coupon.domain.CouponIssuePolicy;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Tag("integration")
@Testcontainers
class CouponIssuePolicyCacheIntegrationTest {
    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:8.0").withExposedPorts(6379);

    private static final CouponIssuePolicy POLICY =
        new CouponIssuePolicy(LocalDateTime.of(2099, 1, 1, 0, 0), LocalDateTime.of(2100, 1, 1, 0, 0), 1, false);

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;

    private final CacheMetrics cacheMetrics = new CacheMetrics();

    @BeforeAll
    static void connect() {
        connectionFactory = new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
    }

    @AfterAll
    static void disconnect() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void 같은_쿠폰의_동시_미스는_한_번_로딩하고_다른_쿠폰은_독립적으로_로딩한다() throws Exception {
        final var firstInstance = cache();
        final var secondInstance = cache();
        final String key = "coupon:1:issue-policy";
        firstInstance.getOrLoad(1L, () -> POLICY);
        redisTemplate.expire(key, Duration.ofMillis(30));
        awaitExpiry(key);

        final var loads = new AtomicInteger();
        final var ownerLoading = new CountDownLatch(1);
        final var finishLoad = new CountDownLatch(1);

        try (final var executor = Executors.newFixedThreadPool(4)) {
            final var owner = executor.submit(() -> firstInstance.getOrLoad(1L, () -> {
                loads.incrementAndGet();
                ownerLoading.countDown();
                await(finishLoad);
                return POLICY;
            }));
            try {
                await(ownerLoading);
                final var waiter = executor.submit(() -> secondInstance.getOrLoad(1L, () -> {
                    loads.incrementAndGet();
                    return POLICY;
                }));
                assertThatThrownBy(() -> waiter.get(100, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);

                final var otherKey = executor.submit(() -> secondInstance.getOrLoad(2L, () -> POLICY));
                assertThat(otherKey.get(2, TimeUnit.SECONDS)).isEqualTo(POLICY);
                finishLoad.countDown();

                assertThat(List.of(owner.get(2, TimeUnit.SECONDS), waiter.get(2, TimeUnit.SECONDS))).containsOnly(POLICY);
                assertThat(loads).hasValue(1);
                assertThat(cacheMetrics.snapshot().couponCacheHits()).isEqualTo(1);
                assertThat(cacheMetrics.snapshot().couponDbReads()).isZero();
            } finally {
                finishLoad.countDown();
            }
        } finally {
            firstInstance.stopRefreshExecutor();
            secondInstance.stopRefreshExecutor();
        }
    }

    @Test
    void 로더_예외를_그대로_전파하고_다음_요청이_다시_로딩할_수_있다() {
        final var cache = cache();
        final var failure = new CoreException(ErrorCode.COUPON_NOT_FOUND_OR_EXPIRED);

        assertThatThrownBy(() -> cache.getOrLoad(3L, () -> {
            throw failure;
        })).isSameAs(failure);
        assertThat(cache.getOrLoad(3L, () -> POLICY)).isEqualTo(POLICY);
        assertThat(redisTemplate.getExpire("coupon:3:issue-policy", TimeUnit.MILLISECONDS)).isBetween(1L, 10_000L);
    }

    @Test
    void 로더와_락_해제가_모두_실패하면_원래_로더_예외를_전파한다() {
        final StringRedisTemplate failingRedis = mock(StringRedisTemplate.class);
        final CoreException loadFailure = new CoreException(ErrorCode.COUPON_NOT_FOUND_OR_EXPIRED);
        final IllegalStateException releaseFailure = new IllegalStateException("락 해제 실패");
        doReturn(List.of("LOAD", "")).when(failingRedis).execute(any(), anyList(), anyString(), anyString(), anyString(), anyString());
        doThrow(releaseFailure).when(failingRedis).execute(any(), anyList(), anyString());
        final CouponIssuePolicyCacheAdapter cache = new CouponIssuePolicyCacheAdapter(failingRedis, JsonMapper.builder().build(),
            new CacheProperties(10_000, 5_000, 0), cacheMetrics);

        assertThatThrownBy(() -> cache.getOrLoad(6L, () -> {
            throw loadFailure;
        })).isSameAs(loadFailure);
    }

    @Test
    void 캐시를_저장한_뒤_락_해제_결과가_null이면_예외를_던진다() {
        final StringRedisTemplate failingRedis = mock(StringRedisTemplate.class);
        doReturn(List.of("LOAD", "")).when(failingRedis).execute(any(), anyList(), anyString(), anyString(), anyString(), anyString());
        doReturn(mock(HashOperations.class)).when(failingRedis).opsForHash();
        doReturn(true).when(failingRedis).expire(anyString(), any(Duration.class));
        doReturn(null).when(failingRedis).execute(any(), anyList(), anyString());
        final CouponIssuePolicyCacheAdapter cache = new CouponIssuePolicyCacheAdapter(failingRedis, JsonMapper.builder().build(),
            new CacheProperties(10_000, 5_000, 0), cacheMetrics);

        assertThatThrownBy(() -> cache.getOrLoad(8L, () -> POLICY)).isInstanceOf(IllegalStateException.class).hasMessage("Lua 결과 null");
    }

    @Test
    void 저장에_실패하면_자기_락을_해제해_다음_요청이_다시_로딩할_수_있다() {
        final StringRedisTemplate failingRedis = spy(redisTemplate);
        final IllegalStateException saveFailure = new IllegalStateException("캐시 저장 실패");
        doThrow(saveFailure).when(failingRedis).opsForHash();
        final CouponIssuePolicyCacheAdapter cache = new CouponIssuePolicyCacheAdapter(failingRedis, JsonMapper.builder().build(),
            new CacheProperties(10_000, 5_000, 0), cacheMetrics);

        assertThatThrownBy(() -> cache.getOrLoad(7L, () -> POLICY)).isSameAs(saveFailure);
        assertThat(redisTemplate.opsForValue().get("coupon:7:issue-policy:lock")).isNull();
        assertThat(cache().getOrLoad(7L, () -> POLICY)).isEqualTo(POLICY);
    }

    @Test
    void 만료된_소유자도_정책을_저장하지만_새_소유자의_락은_지우지_못한다() {
        final var cache = cache();
        final String policyKey = "coupon:4:issue-policy";
        final String lockKey = policyKey + ":lock";

        assertThat(cache.getOrLoad(4L, () -> {
            assertThat(redisTemplate.getExpire(lockKey, TimeUnit.MILLISECONDS)).isBetween(1L, 3_000L);
            assertThat(redisTemplate.expire(lockKey, Duration.ZERO)).isTrue();
            assertThat(redisTemplate.opsForValue().setIfAbsent(lockKey, "next-owner", Duration.ofSeconds(3))).isTrue();
            return POLICY;
        })).isEqualTo(POLICY);
        assertThat(redisTemplate.opsForHash().get(policyKey, "value")).isEqualTo(JsonMapper.builder().build().writeValueAsString(POLICY));
        assertThat(redisTemplate.opsForValue().get(lockKey)).isEqualTo("next-owner");
    }

    @Test
    @Timeout(4)
    void 대기_한도를_넘으면_DB를_읽거나_락을_해제하지_않고_예외를_던진다() {
        final String lockKey = "coupon:5:issue-policy:lock";
        redisTemplate.opsForValue().set(lockKey, "owner", Duration.ofSeconds(10));

        assertThatThrownBy(() -> cache().getOrLoad(5L, () -> {
            throw new AssertionError("대기 한도를 넘은 요청은 DB를 읽지 않아야 합니다.");
        })).isInstanceOf(IllegalStateException.class).hasMessageContaining("timeout");
        assertThat(redisTemplate.opsForValue().get(lockKey)).isEqualTo("owner");
        assertThat(redisTemplate.hasKey("coupon:5:issue-policy")).isFalse();
    }

    @Test
    void 빈_캐시_값은_적중으로_분기하고_JSON_변환에_실패한다() {
        redisTemplate.opsForHash().put("coupon:9:issue-policy", "value", "");
        redisTemplate.opsForHash().put("coupon:9:issue-policy", "fetchedAtMs", Long.toString(System.currentTimeMillis()));

        assertThatThrownBy(() -> cache().getOrLoad(9L, () -> {
            throw new AssertionError("캐시 적중 시 DB를 읽지 않아야 합니다.");
        })).isInstanceOf(JacksonException.class);
    }

    @Test
    void 예상하지_못한_상태가_오면_재시도하지_않고_즉시_실패한다() {
        final StringRedisTemplate redis = mock(StringRedisTemplate.class);
        final JsonMapper mapper = JsonMapper.builder().build();
        doReturn(List.of("UNKNOWN", ""), List.of("HIT", mapper.writeValueAsString(POLICY))).when(redis).execute(any(), anyList(),
            anyString(), anyString(), anyString(), anyString());
        final CouponIssuePolicyCacheAdapter cache =
            new CouponIssuePolicyCacheAdapter(redis, mapper, new CacheProperties(10_000, 5_000, 0), cacheMetrics);

        assertThatThrownBy(() -> cache.getOrLoad(10L, () -> {
            throw new AssertionError("알 수 없는 Lua 상태에서는 DB를 읽지 않아야 합니다.");
        })).isInstanceOf(IllegalStateException.class).hasMessage("예상치 못한 Lua 결과: UNKNOWN");
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, -1_000L, -2L, -2_000L})
    void TTL이_0_이하면_조회_값은_반환하되_Hash를_즉시_만료하고_락을_해제한다(final long ttlMs) {
        final long couponId = 20L - ttlMs;
        final String cacheKey = "coupon:" + couponId + ":issue-policy";

        assertThat(cache(ttlMs).getOrLoad(couponId, () -> POLICY)).isEqualTo(POLICY);
        assertThat(redisTemplate.hasKey(cacheKey)).isFalse();
        assertThat(redisTemplate.hasKey(cacheKey + ":lock")).isFalse();
    }

    @Test
    void 기존_문자열_캐시가_만료되어야_같은_키를_Hash로_저장할_수_있다() {
        final String key = "coupon:50:issue-policy";
        final String json = JsonMapper.builder().build().writeValueAsString(POLICY);
        redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(10));

        assertThatThrownBy(() -> cache().getOrLoad(50L, () -> POLICY)).isInstanceOf(DataAccessException.class)
            .hasStackTraceContaining("WRONGTYPE");

        assertThat(redisTemplate.expire(key, Duration.ZERO)).isTrue();
        assertThat(cache().getOrLoad(50L, () -> POLICY)).isEqualTo(POLICY);
        assertThat(redisTemplate.opsForHash().get(key, "value")).isEqualTo(json);
    }

    @Test
    void Hash는_fresh_구간에는_로딩하지_않고_stale_동시_요청에는_한_번만_갱신한다() throws Exception {
        final var first = cache();
        final var second = cache();
        final String key = "coupon:100:issue-policy";
        final var freshPolicy = new CouponIssuePolicy(POLICY.startsAt(), POLICY.expiredAt(), 2, false);
        final var loading = new CountDownLatch(1);
        final var finish = new CountDownLatch(1);
        final var loads = new AtomicInteger();
        final Supplier<CouponIssuePolicy> loader = () -> {
            loads.incrementAndGet();
            loading.countDown();
            await(finish);
            return freshPolicy;
        };

        try (final var callers = Executors.newFixedThreadPool(4)) {
            final String json = JsonMapper.builder().build().writeValueAsString(POLICY);
            assertThat(first.getOrLoad(100L, () -> POLICY)).isEqualTo(POLICY);
            assertThat(redisTemplate.opsForHash().get(key, "value")).isEqualTo(json);
            assertThat(Long.parseLong((String) redisTemplate.opsForHash().get(key, "fetchedAtMs"))).isPositive();
            assertThat(redisTemplate.getExpire(key, TimeUnit.MILLISECONDS)).isBetween(1L, 10_000L);
            assertThat(first.getOrLoad(100L, loader)).isEqualTo(POLICY);
            assertThat(loads).hasValue(0);
            markStale(key);

            assertThat(first.getOrLoad(100L, loader)).isEqualTo(POLICY);
            await(loading);
            final var requests = callers.<CouponIssuePolicy>invokeAll(Collections.nCopies(8, () -> second.getOrLoad(100L, loader)));
            for (final var request : requests) {
                assertThat(request.get()).isEqualTo(POLICY);
            }
            assertThat(loads).hasValue(1);
            assertThat(cacheMetrics.snapshot().couponCacheHits()).isEqualTo(10);

            finish.countDown();
            awaitUnlock(key);
            assertThat(first.getOrLoad(100L, loader)).isEqualTo(freshPolicy);
            assertThat(redisTemplate.opsForHash().get(key, "value"))
                .isEqualTo(JsonMapper.builder().build().writeValueAsString(freshPolicy));
            assertThat(redisTemplate.opsForHash().get(key, "fetchedAtMs")).isNotEqualTo("0");
        } finally {
            finish.countDown();
            first.stopRefreshExecutor();
            second.stopRefreshExecutor();
        }
    }

    @Test
    void 갱신_실패_후_stale을_유지하고_실제_TTL_만료_후에는_삭제_오류를_전파한다() throws Exception {
        final var cache = cache();
        final String key = "coupon:101:issue-policy";
        final var failures = new AtomicInteger();
        final CoreException missing = new CoreException(ErrorCode.COUPON_NOT_FOUND_OR_EXPIRED);
        final Supplier<CouponIssuePolicy> loader = () -> {
            failures.incrementAndGet();
            throw missing;
        };
        try {
            cache.getOrLoad(101L, () -> POLICY);
            markStale(key);
            assertThat(cache.getOrLoad(101L, loader)).isEqualTo(POLICY);
            awaitUnlock(key);
            assertThat(cache.getOrLoad(101L, loader)).isEqualTo(POLICY);
            awaitUnlock(key);
            assertThat(failures).hasValue(2);

            redisTemplate.expire(key, Duration.ofMillis(30));
            awaitExpiry(key);
            assertThatThrownBy(() -> cache.getOrLoad(101L, loader)).isSameAs(missing);
        } finally {
            cache.stopRefreshExecutor();
        }
    }

    @Test
    void 재고_복구_후에도_갱신이_완료될_때까지_stale_매진_정책을_반환한다() throws Exception {
        final var cache = cache();
        final var soldOut = new CouponIssuePolicy(POLICY.startsAt(), POLICY.expiredAt(), 1, true);
        final String key = "coupon:103:issue-policy";
        try {
            cache.getOrLoad(103L, () -> soldOut);
            markStale(key);

            assertThat(cache.getOrLoad(103L, () -> POLICY).soldOut()).isTrue();
            awaitUnlock(key);
            assertThat(cache.getOrLoad(103L, () -> POLICY).soldOut()).isFalse();
        } finally {
            cache.stopRefreshExecutor();
        }
    }

    private static void markStale(final String key) {
        redisTemplate.opsForHash().put(key, "fetchedAtMs", "0");
    }

    private static void awaitUnlock(final String key) throws InterruptedException {
        awaitExpiry(key + ":lock");
    }

    private static void awaitExpiry(final String key) throws InterruptedException {
        for (int attempt = 0; attempt < 100 && Boolean.TRUE.equals(redisTemplate.hasKey(key)); attempt++) {
            Thread.sleep(10);
        }
        assertThat(redisTemplate.hasKey(key)).isFalse();
    }

    private CouponIssuePolicyCacheAdapter cache() {
        return cache(10_000);
    }

    private CouponIssuePolicyCacheAdapter cache(final long ttlMs) {
        return new CouponIssuePolicyCacheAdapter(redisTemplate, JsonMapper.builder().build(), new CacheProperties(ttlMs, 5_000, 0),
            cacheMetrics);
    }

    private static void await(final CountDownLatch latch) {
        try {
            assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }
}
