package com.book.core.coupon.infrastructure.load;

import com.book.core.coupon.application.port.CouponLoadSimulationPort;
import com.book.core.coupon.infrastructure.cache.CacheProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CouponLoadSimulationAdapter implements CouponLoadSimulationPort {
    private final CacheProperties cacheProperties;

    @Override
    public void simulateCouponReadLatency() {
        final long latencyMs = cacheProperties.simulatedLoadLatencyMs();
        if (latencyMs == 0) {
            return;
        }

        try {
            Thread.sleep(latencyMs);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
