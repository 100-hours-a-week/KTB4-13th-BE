package com.book.core.book.infrastructure.observability;

import com.book.common.config.scheduler.SchedulerLockListener;
import com.book.core.book.application.result.NewBookImportOutcome;
import com.book.core.book.application.result.NewBookImportReport;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NewBookImportMetrics {
    private final MeterRegistry registry;

    public NewBookImportMetrics(final MeterRegistry registry, @Value("${app.new-book.jeongbo-naru-daily-limit:500}") final int dailyLimit) {
        this.registry = registry;
        Gauge.builder("book.new_import.detail.daily_limit", () -> dailyLimit).register(registry);
        SchedulerLockListener.registerLock(registry, "new-book-import");
        for (final String result : List.of("completed", "failed")) {
            registry.counter("book.new_import.runs", "result", result);
        }
        for (final String result : List.of("completed", "failed")) {
            Timer.builder("book.new_import.duration").tag("result", result).register(registry);
        }
        registry.counter("book.new_import.candidates");
        registry.counter("book.new_import.detail.calls");
        for (final NewBookImportOutcome outcome : NewBookImportOutcome.values()) {
            registry.counter("book.new_import.outcomes", "outcome", outcome.name());
        }
        registry.counter("book.new_import.external.errors", "provider", "national_library", "code", "011");
        for (final String code : List.of("authErr", "vitalizationErr", "isbnMpngErr")) {
            registry.counter("book.new_import.external.errors", "provider", "jeongbo_naru", "code", code);
        }
    }

    public void recordRun(final NewBookImportReport report) {
        final String result = report.failed() ? "failed" : "completed";
        registry.counter("book.new_import.runs", "result", result).increment();
        Timer.builder("book.new_import.duration").tag("result", result).register(registry).record(Duration.ofMillis(report.elapsedMs()));
        registry.counter("book.new_import.candidates").increment(report.candidateCount());
        registry.counter("book.new_import.detail.calls").increment(report.detailCallCount());
        report.outcomes()
            .forEach((final var outcome, final var count) -> registry.counter("book.new_import.outcomes", "outcome", outcome.name())
                .increment(count));
    }

    public void recordExternalError(final String provider, final String code) {
        registry.counter("book.new_import.external.errors", "provider", provider, "code", code == null ? "unknown" : code).increment();
    }
}
