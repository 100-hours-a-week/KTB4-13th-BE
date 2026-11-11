package com.book.core.product.infrastructure.client;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.product.application.port.ProductInventoryLockPort;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.product.inventory.redis-lock", name = "enabled", havingValue = "true")
public class ProductInventoryRedisLockAdapter implements ProductInventoryLockPort {
    private static final String LOCK_KEY_PREFIX = "product:inventory:lock:";
    private static final long LOCK_WAIT_MILLIS = 2_000L;
    private static final long LOCK_TTL_MILLIS = 30_000L;
    private static final long LOCK_RETRY_MIN_MILLIS = 20L;
    private static final long LOCK_RETRY_MAX_MILLIS = 50L;
    private static final RedisScript<Long> RELEASE_LOCK_SCRIPT = RedisScript.of(new ClassPathResource("lua/release-lock.lua"), Long.class);

    private final StringRedisTemplate redisTemplate;
    private final MeterRegistry meterRegistry;

    @Override
    public Map<Long, String> acquireAll(final List<Long> productIds) {
        final Timer.Sample timer = Timer.start(meterRegistry);
        final long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(LOCK_WAIT_MILLIS);
        final Map<Long, String> acquiredTokens = new LinkedHashMap<>();
        try {
            final List<Long> sortedProductIds = productIds.stream().distinct().sorted().toList();
            for (final Long productId : sortedProductIds) {
                acquire(productId, deadline, acquiredTokens);
            }
            recordAcquisition(timer, "acquired");
            return Collections.unmodifiableMap(new LinkedHashMap<>(acquiredTokens));
        } catch (final CoreException exception) {
            releaseAcquiredLocks(acquiredTokens);
            final String result = exception.errorCode() == ErrorCode.PRODUCT_INVENTORY_LOCK_TIMEOUT ? "timeout" : "unavailable";
            recordAcquisition(timer, result);
            throw exception;
        } catch (final RuntimeException exception) {
            releaseAcquiredLocks(acquiredTokens);
            recordAcquisition(timer, "unavailable");
            throw new CoreException(ErrorCode.PRODUCT_INVENTORY_LOCK_UNAVAILABLE, exception);
        }
    }

    @Override
    public void releaseAll(final Map<Long, String> lockTokensByProductId) {
        final List<Map.Entry<Long, String>> locks = new ArrayList<>(lockTokensByProductId.entrySet());
        Collections.reverse(locks);
        for (final Map.Entry<Long, String> lock : locks) {
            try {
                redisTemplate.execute(RELEASE_LOCK_SCRIPT, List.of(lockKey(lock.getKey())), lock.getValue());
            } catch (final RuntimeException exception) {
                log.atError().addKeyValue("productId", lock.getKey()).log("[INVENTORY] Redis 재고 락 해제에 실패했습니다. TTL 만료까지 락이 유지됩니다.",
                    exception);
            }
        }
    }

    private void acquire(final Long productId, final long deadline, final Map<Long, String> acquiredTokens) {
        final String token = UUID.randomUUID().toString();
        final String key = lockKey(productId);
        while (System.nanoTime() < deadline) {
            final Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, token, Duration.ofMillis(LOCK_TTL_MILLIS));
            if (Boolean.TRUE.equals(acquired)) {
                if (System.nanoTime() >= deadline) {
                    releaseLock(productId, token);
                    throw new CoreException(ErrorCode.PRODUCT_INVENTORY_LOCK_TIMEOUT);
                }
                acquiredTokens.put(productId, token);
                return;
            }
            pauseBeforeRetry(deadline);
        }
        throw new CoreException(ErrorCode.PRODUCT_INVENTORY_LOCK_TIMEOUT);
    }

    private void pauseBeforeRetry(final long deadline) {
        final long delayMillis = ThreadLocalRandom.current().nextLong(LOCK_RETRY_MIN_MILLIS, LOCK_RETRY_MAX_MILLIS + 1);
        final long remainingNanos = deadline - System.nanoTime();
        if (remainingNanos <= 0) {
            return;
        }
        final long sleepNanos = Math.min(TimeUnit.MILLISECONDS.toNanos(delayMillis), remainingNanos);
        try {
            TimeUnit.NANOSECONDS.sleep(sleepNanos);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CoreException(ErrorCode.PRODUCT_INVENTORY_LOCK_UNAVAILABLE, exception);
        }
    }

    private void releaseAcquiredLocks(final Map<Long, String> acquiredTokens) {
        releaseAll(acquiredTokens);
    }

    private void releaseLock(final Long productId, final String token) {
        releaseAll(Map.of(productId, token));
    }

    private void recordAcquisition(final Timer.Sample timer, final String result) {
        timer.stop(Timer.builder("product.inventory.redis.lock.acquire").tag("result", result).register(meterRegistry));
    }

    private String lockKey(final Long productId) {
        return LOCK_KEY_PREFIX + productId;
    }
}
