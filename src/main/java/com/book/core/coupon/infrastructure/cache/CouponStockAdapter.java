package com.book.core.coupon.infrastructure.cache;

import static com.book.core.coupon.infrastructure.cache.RedisLuaSupport.listLuaScript;
import static com.book.core.coupon.infrastructure.cache.RedisLuaSupport.runForStrings;
import static com.book.core.coupon.infrastructure.cache.RedisLuaSupport.longLuaScript;
import static com.book.core.coupon.infrastructure.cache.RedisLuaSupport.runForLong;

import com.book.core.coupon.application.port.CouponStockPort;
import com.book.core.coupon.application.port.CouponStockSnapshot;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CouponStockAdapter implements CouponStockPort {
    private static final RedisScript<Long> INITIALIZE_STOCK_SCRIPT =
        RedisScript.of(new ClassPathResource("lua/initialize-coupon-stock.lua"), Long.class);

    @SuppressWarnings("rawtypes")
    private static final RedisScript<List> RECONCILIATION_SCRIPT = listLuaScript("lua/read-coupon-reconciliation.lua");

    private static final RedisScript<Long> REPAIR = longLuaScript("lua/repair-coupon-reconciliation.lua");

    private final StringRedisTemplate redisTemplate;
    private final LoadingCache<String, Boolean> couponSoldOutCache;
    private final SoldOutProperties soldOutProperties;

    @Override
    public void initialize(final Long couponId, final int quantity) {
        redisTemplate.execute(INITIALIZE_STOCK_SCRIPT, stockKeys(couponId), Integer.toString(quantity));
        couponSoldOutCache.invalidate(soldOutKey(couponId));
    }

    @Override
    public boolean hasSoldOutSignal(final Long couponId) {
        return Boolean.TRUE.equals(couponSoldOutCache.get(soldOutKey(couponId)));
    }

    @Override
    public CouponStockSnapshot readSnapshot(final long couponId) {
        final List<String> values = runForStrings(redisTemplate, RECONCILIATION_SCRIPT,
            List.of("coupon:" + couponId + ":stock", soldOutKey(couponId), "coupon:" + couponId + ":reservations"));
        final String stock = values.getFirst();
        return new CouponStockSnapshot(stock.isEmpty() ? null : Integer.valueOf(stock), "1".equals(values.get(1)),
            values.subList(2, values.size()).stream().map(Long::valueOf).collect(Collectors.toUnmodifiableSet()));
    }

    @Override
    public boolean repairSnapshot(final long couponId, final CouponStockSnapshot observed, final Set<Long> missingUserIds) {
        final var args = new ArrayList<>();
        args.add(observed.quantity());
        args.add(observed.soldOutSignalPresent() ? 1 : 0);
        args.add(soldOutProperties.ttlSeconds());
        args.add(observed.reservedUserIds().size());
        args.addAll(observed.reservedUserIds());
        args.addAll(missingUserIds);
        final boolean repaired = runForLong(redisTemplate, REPAIR,
            List.of("coupon:" + couponId + ":stock", soldOutKey(couponId), "coupon:" + couponId + ":reservations"), args.toArray()) == 1;
        if (repaired) {
            couponSoldOutCache.invalidate(soldOutKey(couponId));
            log.atInfo().addKeyValue("couponId", couponId).addKeyValue("restoredUsers", missingUserIds.size()).log("[COUPON] Redis 대사 보정");
        }
        return repaired;
    }

    private List<String> stockKeys(final Long couponId) {
        return List.of("coupon:" + couponId + ":stock", soldOutKey(couponId));
    }

    private String soldOutKey(final Long couponId) {
        return "coupon:" + couponId + ":sold_out";
    }
}
