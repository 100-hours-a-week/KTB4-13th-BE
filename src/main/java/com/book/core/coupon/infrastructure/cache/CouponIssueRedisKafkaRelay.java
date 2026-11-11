package com.book.core.coupon.infrastructure.cache;

import static com.book.core.coupon.infrastructure.cache.RedisLuaSupport.listLuaScript;
import static com.book.core.coupon.infrastructure.cache.RedisLuaSupport.longLuaScript;
import static com.book.core.coupon.infrastructure.cache.RedisLuaSupport.runForLong;
import static com.book.core.coupon.infrastructure.cache.RedisLuaSupport.runForStrings;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("coupon-redis-kafka & !coupon-kafka")
@RequiredArgsConstructor
class CouponIssueRedisKafkaRelay {
    static final String GROUP = "coupon-kafka-relay";
    private final RedisScript<Long> initScript = longLuaScript("lua/init-coupon-issue-stream.lua");
    @SuppressWarnings("rawtypes")
    private final RedisScript<List> readScript = listLuaScript("lua/read-coupon-issue-stream.lua");
    private final StringRedisTemplate redis;
    private final KafkaTemplate<String, String> kafka;
    private final String consumer = UUID.randomUUID().toString();
    private String claimCursor = "0-0";

    @Value("${coupon.issuance.stream}")
    private String stream;
    @Value("${coupon.kafka.topic}")
    private String topic;

    @PostConstruct
    void initialize() {
        runForLong(redis, initScript, List.of(stream), GROUP);
    }

    @Scheduled(fixedDelayString = "${coupon.kafka.relay-delay-ms:100}", scheduler = "couponKafkaExecutor")
    void relay() {
        try {
            final List<String> entries = runForStrings(redis, readScript, List.of(stream), GROUP, consumer, claimCursor);
            claimCursor = entries.getFirst();
            final List<CompletableFuture<?>> deliveries = new ArrayList<>();
            final List<String> ids = new ArrayList<>();
            for (int index = 1; index < entries.size(); index += 2) {
                final String id = entries.get(index);
                deliveries.add(kafka.send(topic, id, entries.get(index + 1)));
                ids.add(id);
            }
            if (!ids.isEmpty()) {
                CompletableFuture.allOf(deliveries.toArray(CompletableFuture<?>[]::new)).orTimeout(12, TimeUnit.SECONDS).join();
                redis.opsForStream().acknowledge(stream, GROUP, ids.toArray(String[]::new));
            }
        } catch (final RuntimeException failure) {
            log.atWarn().addKeyValue("errorType", failure.getClass().getSimpleName()).log("[COUPON] 발행 재시도");
        }
    }
}
