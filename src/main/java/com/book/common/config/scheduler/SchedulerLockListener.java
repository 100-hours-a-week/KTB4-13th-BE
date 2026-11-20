package com.book.common.config.scheduler;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutorListener;

@Slf4j
@RequiredArgsConstructor
public class SchedulerLockListener implements LockingTaskExecutorListener {
    private final MeterRegistry registry;

    public static void registerLock(final MeterRegistry registry, final String lockName) {
        attempts(registry, lockName, "acquired");
        attempts(registry, lockName, "not_acquired");
    }

    @Override
    public void onLockAcquired(final LockConfiguration configuration) {
        attempts(registry, configuration.getName(), "acquired").increment();
    }

    @Override
    public void onLockNotAcquired(final LockConfiguration configuration) {
        attempts(registry, configuration.getName(), "not_acquired").increment();
        log.atInfo().addKeyValue("lockName", configuration.getName())
            .log("[SCHEDULER] 다른 인스턴스가 실행 중이어서 건너뜁니다. lockName={}", configuration.getName());
    }

    private static Counter attempts(final MeterRegistry registry, final String lockName, final String result) {
        return registry.counter("scheduler.lock.attempts", "lock_name", lockName, "result", result);
    }
}
