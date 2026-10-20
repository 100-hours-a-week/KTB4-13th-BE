package com.book.core.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.book.common.exception.CoreException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PgOperationTest {
    @Test
    void 같은_요청은_검증하고_다른_입력은_거절한다() {
        final PgOperation operation = new PgOperation("order", "request", "input", PgOperationType.CANCEL);
        operation.verifyInput("order", "input", PgOperationType.CANCEL);
        assertThatThrownBy(() -> operation.verifyInput("order", "changed", PgOperationType.CANCEL)).isInstanceOf(CoreException.class);
    }

    @Test
    void 만료된_미완료_요청은_PG를_다시_호출할_수_없다() {
        final PgOperation operation = new PgOperation("order", "request", "input", PgOperationType.CANCEL);
        ReflectionTestUtils.setField(operation, "requestedAt", Instant.now().minus(15, ChronoUnit.DAYS));
        assertThatThrownBy(operation::verifyRetryWindow).isInstanceOf(CoreException.class);
        operation.complete(42L);
        operation.verifyRetryWindow();
        assertThat(operation.resultId()).isEqualTo(42L);
    }

    @Test
    void 요청별_PG_키는_서로_다르다() {
        final PgOperation first = new PgOperation("order", "request", "input", PgOperationType.CANCEL);
        final PgOperation second = new PgOperation("order", "other", "input", PgOperationType.CANCEL);
        assertThat(first.pgIdempotencyKey()).isNotEqualTo(second.pgIdempotencyKey());
    }
}
