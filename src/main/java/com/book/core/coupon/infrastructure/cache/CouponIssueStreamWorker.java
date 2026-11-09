package com.book.core.coupon.infrastructure.cache;

import static com.book.core.coupon.infrastructure.cache.CouponIssueStreamAdapter.STREAM;
import static com.book.core.coupon.infrastructure.cache.RedisLuaSupport.listLuaScript;
import static com.book.core.coupon.infrastructure.cache.RedisLuaSupport.longLuaScript;
import static com.book.core.coupon.infrastructure.cache.RedisLuaSupport.runForLong;
import static com.book.core.coupon.infrastructure.cache.RedisLuaSupport.runForStrings;

import com.book.core.coupon.application.command.AcceptedCouponIssue;
import com.book.core.coupon.application.usecase.PersistAcceptedCouponIssueUseCase;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@Profile("coupon-streams & coupon-streams-worker")
@RequiredArgsConstructor
public class CouponIssueStreamWorker {
    private static final String GROUP = "coupon-db";
    private final RedisScript<Long> initScript = longLuaScript("lua/init-coupon-issue-stream.lua");
    @SuppressWarnings("rawtypes")
    private final RedisScript<List> readScript = listLuaScript("lua/read-coupon-issue-stream.lua");
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final PersistAcceptedCouponIssueUseCase persistence;
    private final MeterRegistry meters;
    @Value("${coupon.streams.workers:1}")
    private int workers;
    private ScheduledExecutorService executor;
    private Timer commitDelay;

    @PostConstruct
    void initialize() {
        if (workers < 1 || workers > 4) {
            throw new IllegalArgumentException("실험 워커 수는 1~4개여야 합니다.");
        }
        runForLong(redis, initScript, List.of(STREAM), GROUP);
        commitDelay = Timer.builder("coupon.issuance.commit.delay")
            .serviceLevelObjectives(Duration.ofMillis(10), Duration.ofMillis(50), Duration.ofMillis(100), Duration.ofMillis(250),
                Duration.ofMillis(500), Duration.ofMillis(750), Duration.ofSeconds(1), Duration.ofMillis(1500), Duration.ofSeconds(2),
                Duration.ofSeconds(3), Duration.ofSeconds(4), Duration.ofSeconds(5), Duration.ofSeconds(6), Duration.ofSeconds(8),
                Duration.ofSeconds(10), Duration.ofSeconds(12), Duration.ofSeconds(15), Duration.ofSeconds(18), Duration.ofSeconds(20),
                Duration.ofSeconds(25), Duration.ofSeconds(30), Duration.ofSeconds(40), Duration.ofSeconds(60), Duration.ofSeconds(120))
            .register(meters);
        executor = Executors.newScheduledThreadPool(workers, Thread.ofPlatform().daemon().name("coupon-streams-", 1).factory());
        for (int index = 0; index < workers; index++) {
            executor.scheduleWithFixedDelay(new Consumer(), 0, 20, TimeUnit.MILLISECONDS);
        }
    }

    @PreDestroy
    void stop() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private final class Consumer implements Runnable {
        private final String name = UUID.randomUUID().toString();
        private String claimCursor = "0-0";

        @Override
        public void run() {
            try {
                final List<String> entries = runForStrings(redis, readScript, List.of(STREAM), GROUP, name, claimCursor);
                claimCursor = entries.getFirst();
                for (int index = 1; index < entries.size(); index += 2) {
                    persistAndAck(entries.get(index), entries.get(index + 1));
                }
            } catch (final RuntimeException failure) {
                log.atWarn().log("[COUPON] 발급 메시지 조회 실패. type={}", failure.getClass().getSimpleName());
            }
        }
    }

    private void persistAndAck(final String id, final String payload) {
        try {
            final AcceptedCouponIssue issue = objectMapper.readValue(payload, AcceptedCouponIssue.class);
            if (persistence.execute(issue)) {
                final long acceptedAt = Long.parseLong(id.substring(0, id.indexOf('-')));
                commitDelay.record(Duration.ofMillis(Math.max(0L, System.currentTimeMillis() - acceptedAt)));
            } else {
                meters.counter("coupon.issuance.replayed").increment();
            }
            redis.opsForStream().acknowledge(STREAM, GROUP, id);
        } catch (final RuntimeException failure) {
            meters.counter("coupon.issuance.worker.failures").increment();
            log.atWarn().log("[COUPON] 발급 메시지 처리 실패. id={}, type={}", id, failure.getClass().getSimpleName());
        }
    }
}
