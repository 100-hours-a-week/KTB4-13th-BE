package com.book.core.coupon.infrastructure.cache;

import static com.book.core.coupon.infrastructure.cache.RedisLuaSupport.listLuaScript;
import static com.book.core.coupon.infrastructure.cache.RedisLuaSupport.longLuaScript;
import static com.book.core.coupon.infrastructure.cache.RedisLuaSupport.runForLong;
import static com.book.core.coupon.infrastructure.cache.RedisLuaSupport.runForStrings;

import com.book.core.coupon.application.port.CouponCacheRepositoryPort;
import com.book.core.coupon.application.service.CacheMetrics;
import com.book.core.coupon.domain.CouponIssuePolicy;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponIssuePolicyCacheAdapter implements CouponCacheRepositoryPort {
    private static final int MAX_RETRIES = 50;
    private static final long WAIT_BACKOFF_MS = 20L;
    private static final long LOCK_TTL_MS = 3_000L;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final CacheProperties cacheProperties;
    private final CacheMetrics cacheMetrics;

    private final ExecutorService backgroundExecutor = Executors.newFixedThreadPool(4, (final Runnable runnable) -> {
        final Thread thread = new Thread(runnable, "coupon-cache-swr-refresh");
        thread.setDaemon(true);
        return thread;
    });

    @SuppressWarnings("rawtypes")
    private final RedisScript<List> lookupScript = listLuaScript("lua/cache-single-flight-swr.lua");
    private final RedisScript<Long> releaseLockScript = longLuaScript("lua/release-lock.lua");

    @Override
    public CouponIssuePolicy getOrLoad(final Long couponId, final Supplier<CouponIssuePolicy> loader) {
        final String cacheKey = "coupon:" + couponId + ":issue-policy";
        return getOrLoad(couponId, cacheKey, cacheKey + ":lock", loader);
    }

    private CouponIssuePolicy getOrLoad(final Long couponId, final String cacheKey, final String lockKey,
        final Supplier<CouponIssuePolicy> loader) {
        final String token = UUID.randomUUID().toString();
        for (int retry = 0; retry < MAX_RETRIES; retry++) {
            final long now = Instant.now().toEpochMilli();
            final List<String> result =
                runForStrings(redisTemplate, lookupScript, List.of(cacheKey, lockKey), now, cacheProperties.freshMs(), token, LOCK_TTL_MS);

            switch (result.getFirst()) {
                case "HIT" -> {
                    cacheMetrics.incrementCouponCacheHit();
                    return objectMapper.readValue(result.get(1), CouponIssuePolicy.class);
                }
                case "STALE_REFRESH" -> {
                    cacheMetrics.incrementCouponCacheHit();
                    backgroundExecutor.execute(() -> {
                        try {
                            fillCache(couponId, cacheKey, lockKey, token, loader);
                        } catch (final Exception exception) {
                            log.atWarn().addKeyValue("couponId", couponId).addKeyValue("cause", exception.getMessage())
                                .log("[COUPON] 발급 정책 캐시의 SWR 백그라운드 갱신에 실패했습니다. couponId={}, cause={}", couponId, exception.getMessage());
                        }
                    });
                    return objectMapper.readValue(result.get(1), CouponIssuePolicy.class);
                }
                case "LOAD" -> {
                    return fillCache(couponId, cacheKey, lockKey, token, loader);
                }
                case "WAIT" -> waitBackoff();
                default -> throw new IllegalStateException("예상치 못한 Lua 결과: " + result.getFirst());
            }
        }
        throw new IllegalStateException("쿠폰 캐시 채우기 timeout (key=" + cacheKey + ")");
    }

    /**
     * 캐시를 채우고 락을 해제합니다.<br>
     * 로딩과 락 해제가 모두 실패하면 로딩 예외를 보존합니다.
     */
    private CouponIssuePolicy fillCache(final Long couponId, final String cacheKey, final String lockKey, final String token,
        final Supplier<CouponIssuePolicy> loader) {
        boolean cacheFilled = false;
        try {
            final CouponIssuePolicy policy = loader.get();
            redisTemplate.<String, String>opsForHash().putAll(cacheKey,
                Map.of("value", objectMapper.writeValueAsString(policy), "fetchedAtMs", String.valueOf(Instant.now().toEpochMilli())));
            redisTemplate.expire(cacheKey, Duration.ofMillis(cacheProperties.ttlMs()));
            cacheFilled = true;
            return policy;
        } finally {
            try {
                runForLong(redisTemplate, releaseLockScript, List.of(lockKey), token);
            } catch (final RuntimeException exception) {
                if (cacheFilled) {
                    throw exception;
                }
                final String cause = exception.getClass().getSimpleName();
                log.atWarn().addKeyValue("couponId", couponId).addKeyValue("cause", cause)
                    .log("[COUPON] 발급 정책 캐시의 락 해제에 실패했습니다. couponId={}, cause={}", couponId, cause);
            }
        }
    }

    /**
     * 애플리케이션 종료 시 레디스 락을 해제하지 않으면 다른 인스턴스에서 캐시 갱신이 불가능해질 수 있으므로<br>
     * 백그라운드 갱신 스레드풀을 종료하여 락 해제를 시도합니다.
     */
    @PreDestroy
    void stopRefreshExecutor() {
        backgroundExecutor.shutdown();
    }

    /**
     * 캐시 조회 시 다른 스레드가 갱신 중이면 일정 시간 대기 후 재시도합니다.
     */
    private void waitBackoff() {
        try {
            Thread.sleep(WAIT_BACKOFF_MS);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
