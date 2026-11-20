package com.book.core.book.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.book.application.result.NewBookImportOutcome;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class NewBookImportMetricsTest {
    @Test
    void 실행_전에도_카운터를_0으로_노출한다() {
        final var registry = new SimpleMeterRegistry();

        new NewBookImportMetrics(registry, 30);

        assertThat(
            registry.get("scheduler.lock.attempts").tag("lock_name", "new-book-import").tag("result", "not_acquired").counter().count())
            .isZero();
        assertThat(registry.get("book.new_import.outcomes").counters()).hasSize(NewBookImportOutcome.values().length);
        assertThat(registry.get("book.new_import.external.errors").tag("code", "isbnMpngErr").counter().count()).isZero();
        assertThat(registry.get("book.new_import.detail.daily_limit").gauge().value()).isEqualTo(30);
    }

    @Test
    void 외부_오류_코드가_없으면_unknown으로_센다() {
        final var registry = new SimpleMeterRegistry();

        new NewBookImportMetrics(registry, 30).recordExternalError("jeongbo_naru", null);

        assertThat(registry.get("book.new_import.external.errors").tag("code", "unknown").counter().count()).isEqualTo(1);
    }
}
