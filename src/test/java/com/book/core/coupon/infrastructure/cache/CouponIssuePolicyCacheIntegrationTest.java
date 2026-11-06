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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
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
            } finally {
                finishLoad.countDown();
            }
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
    void 로더와_락_해제가_모두_실패하면_락_해제_예외를_전파한다() {
        final StringRedisTemplate failingRedis = mock(StringRedisTemplate.class);
        final IllegalStateException releaseFailure = new IllegalStateException("락 해제 실패");
        doReturn(List.of("LOAD", "")).when(failingRedis).execute(any(), anyList(), anyString(), anyString());
        doThrow(releaseFailure).when(failingRedis).execute(any(), anyList(), anyString());
        final CouponIssuePolicyCacheAdapter cache =
            new CouponIssuePolicyCacheAdapter(failingRedis, JsonMapper.builder().build(), new CacheProperties(10_000, 0), cacheMetrics);

        assertThatThrownBy(() -> cache.getOrLoad(6L, () -> {
            throw new CoreException(ErrorCode.COUPON_NOT_FOUND_OR_EXPIRED);
        })).isSameAs(releaseFailure);
    }

    @Test
    void 락_해제_결과가_null이면_예외를_던진다() {
        final StringRedisTemplate failingRedis = mock(StringRedisTemplate.class);
        doReturn(List.of("LOAD", "")).when(failingRedis).execute(any(), anyList(), anyString(), anyString());
        doReturn(null).when(failingRedis).execute(any(), anyList(), anyString());
        final CouponIssuePolicyCacheAdapter cache =
            new CouponIssuePolicyCacheAdapter(failingRedis, JsonMapper.builder().build(), new CacheProperties(10_000, 0), cacheMetrics);

        assertThatThrownBy(() -> cache.getOrLoad(8L, () -> {
            throw new CoreException(ErrorCode.COUPON_NOT_FOUND_OR_EXPIRED);
        })).isInstanceOf(IllegalStateException.class).hasMessage("Lua 결과 null");
    }

    @Test
    void 저장에_실패하면_자기_락을_해제해_다음_요청이_다시_로딩할_수_있다() {
        final StringRedisTemplate failingRedis = spy(redisTemplate);
        final IllegalStateException saveFailure = new IllegalStateException("캐시 저장 실패");
        doThrow(saveFailure).when(failingRedis).execute(any(), anyList(), anyString(), anyString(), anyString());
        final CouponIssuePolicyCacheAdapter cache =
            new CouponIssuePolicyCacheAdapter(failingRedis, JsonMapper.builder().build(), new CacheProperties(10_000, 0), cacheMetrics);

        assertThatThrownBy(() -> cache.getOrLoad(7L, () -> POLICY)).isSameAs(saveFailure);
        assertThat(redisTemplate.opsForValue().get("coupon:7:issue-policy:lock")).isNull();
        assertThat(cache().getOrLoad(7L, () -> POLICY)).isEqualTo(POLICY);
    }

    @Test
    void 만료된_소유자는_정책을_저장하거나_새_소유자의_락을_지우지_못한다() {
        final var cache = cache();
        final String policyKey = "coupon:4:issue-policy";
        final String lockKey = policyKey + ":lock";

        assertThat(cache.getOrLoad(4L, () -> {
            assertThat(redisTemplate.getExpire(lockKey, TimeUnit.MILLISECONDS)).isBetween(1L, 3_000L);
            assertThat(redisTemplate.expire(lockKey, Duration.ZERO)).isTrue();
            assertThat(redisTemplate.opsForValue().setIfAbsent(lockKey, "next-owner", Duration.ofSeconds(3))).isTrue();
            return POLICY;
        })).isEqualTo(POLICY);
        assertThat(redisTemplate.opsForValue().get(policyKey)).isNull();
        assertThat(redisTemplate.opsForValue().get(lockKey)).isEqualTo("next-owner");
    }

    @Test
    @Timeout(4)
    void 대기_한도를_넘으면_DB를_읽거나_락을_해제하지_않고_예외를_던진다() {
        final String lockKey = "coupon:5:issue-policy:lock";
        redisTemplate.opsForValue().set(lockKey, "owner", Duration.ofSeconds(10));

        assertThatThrownBy(() -> cache().getOrLoad(5L, () -> {
            throw new AssertionError("대기 한도를 넘은 요청은 DB를 읽지 않아야 합니다.");
        })).isInstanceOf(IllegalStateException.class).hasMessageContaining("대기 시간 초과");
        assertThat(redisTemplate.opsForValue().get(lockKey)).isEqualTo("owner");
        assertThat(redisTemplate.opsForValue().get("coupon:5:issue-policy")).isNull();
    }

    @Test
    void 빈_캐시_값은_적중으로_분기하고_JSON_변환에_실패한다() {
        redisTemplate.opsForValue().set("coupon:9:issue-policy", "", Duration.ofSeconds(10));

        assertThatThrownBy(() -> cache().getOrLoad(9L, () -> {
            throw new AssertionError("캐시 적중 시 DB를 읽지 않아야 합니다.");
        })).isInstanceOf(JacksonException.class);
    }

    @Test
    void 예상하지_못한_상태가_오면_다음_결과를_확인한다() {
        final StringRedisTemplate redis = mock(StringRedisTemplate.class);
        final JsonMapper mapper = JsonMapper.builder().build();
        doReturn(List.of("UNKNOWN", ""), List.of("HIT", mapper.writeValueAsString(POLICY))).when(redis).execute(any(), anyList(),
            anyString(), anyString());
        final CouponIssuePolicyCacheAdapter cache =
            new CouponIssuePolicyCacheAdapter(redis, mapper, new CacheProperties(10_000, 0), cacheMetrics);

        assertThat(cache.getOrLoad(10L, () -> {
            throw new AssertionError("캐시 적중 시 DB를 읽지 않아야 합니다.");
        })).isEqualTo(POLICY);
    }

    @ParameterizedTest
    @ValueSource(longs = {-1L, -1_000L})
    void 참고_구현에서_영구_만료값으로_취급하는_TTL은_만료_없이_저장한다(final long ttlMs) {
        final long couponId = 11L - ttlMs;
        assertThat(cache(ttlMs).getOrLoad(couponId, () -> POLICY)).isEqualTo(POLICY);
        assertThat(redisTemplate.getExpire("coupon:" + couponId + ":issue-policy")).isEqualTo(-1L);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -2L, -2_000L})
    void 그_외_양수가_아닌_TTL은_Redis가_거절하고_락을_해제한다(final long ttlMs) {
        final long couponId = 20L - ttlMs;
        final String cacheKey = "coupon:" + couponId + ":issue-policy";

        assertThatThrownBy(() -> cache(ttlMs).getOrLoad(couponId, () -> POLICY)).isInstanceOf(DataAccessException.class)
            .hasStackTraceContaining("invalid expire time");
        assertThat(redisTemplate.opsForValue().get(cacheKey)).isNull();
        assertThat(redisTemplate.opsForValue().get(cacheKey + ":lock")).isNull();
    }

    private CouponIssuePolicyCacheAdapter cache() {
        return cache(10_000);
    }

    private CouponIssuePolicyCacheAdapter cache(final long ttlMs) {
        return new CouponIssuePolicyCacheAdapter(redisTemplate, JsonMapper.builder().build(), new CacheProperties(ttlMs, 0), cacheMetrics);
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
