package com.book.core.payment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.book.common.exception.CoreException;
import com.book.core.payment.application.port.PaymentClient;
import com.book.core.payment.application.result.PgPaymentResult;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.enums.PaymentMethod;
import com.book.core.payment.domain.enums.PaymentState;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class RequestPgCancelUseCaseTest {
    private final PaymentClient client = mock(PaymentClient.class);
    private final RequestPgCancelUseCase useCase = new RequestPgCancelUseCase(client);
    private final Payment payment =
        new Payment(1L, 2L, BigDecimal.TEN, -1L, BigDecimal.ZERO, BigDecimal.ZERO, 1L, BigDecimal.TEN, PaymentState.SUCCESS);

    @Test
    void 해당_금액과_잔액의_취소_거래만_선택한다() {
        payment.success("key", PaymentMethod.CARD, "1234");
        final var cancellation =
            new PgPaymentResult.Cancellation("cancel-key", BigDecimal.TEN, BigDecimal.ZERO, "DONE", OffsetDateTime.now());
        when(client.cancel("key", BigDecimal.TEN, "request")).thenReturn(new PgPaymentResult("key", "order", BigDecimal.TEN,
            BigDecimal.ZERO, "CANCELED", PaymentMethod.CARD, "1234", List.of(cancellation)));
        assertThat(useCase.execute("order", payment, BigDecimal.TEN, BigDecimal.TEN, "request").transactionKey()).isEqualTo("cancel-key");
    }

    @Test
    void 다른_취소_응답은_내부_환불에_사용하지_않는다() {
        payment.success("key", PaymentMethod.CARD, "1234");
        when(client.cancel("key", BigDecimal.TEN, "request")).thenReturn(
            new PgPaymentResult("key", "other", BigDecimal.TEN, BigDecimal.ZERO, "CANCELED", PaymentMethod.CARD, "1234", List.of()));
        assertThatThrownBy(() -> useCase.execute("order", payment, BigDecimal.TEN, BigDecimal.TEN, "request"))
            .isInstanceOf(CoreException.class);
    }

    @Test
    void 영원_환불은_PG를_호출하지_않는다() {
        assertThat(useCase.execute("order", payment, BigDecimal.ZERO, BigDecimal.ZERO, "request").transactionKey()).isEmpty();
        verifyNoInteractions(client);
    }

    @Test
    void 소수원_또는_음수_환불은_전송하지_않는다() {
        assertThatThrownBy(() -> useCase.execute("order", payment, new BigDecimal("0.1"), BigDecimal.TEN, "request"))
            .isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> useCase.execute("order", payment, BigDecimal.valueOf(-1), BigDecimal.TEN, "request"))
            .isInstanceOf(CoreException.class);
        verifyNoInteractions(client);
    }
}
