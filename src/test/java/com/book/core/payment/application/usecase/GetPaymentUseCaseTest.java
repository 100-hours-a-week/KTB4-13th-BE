package com.book.core.payment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.enums.PaymentState;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetPaymentUseCaseTest {
    private final PaymentRepositoryPort payments = mock(PaymentRepositoryPort.class);
    private final GetPaymentUseCase useCase = new GetPaymentUseCase(payments);

    @Test
    void 문자열_주문키로_결제를_조회한다() {
        final Payment payment =
            new Payment(1L, 2L, BigDecimal.TEN, -1L, BigDecimal.ZERO, BigDecimal.ZERO, 1L, BigDecimal.TEN, PaymentState.READY);
        when(payments.findByOrderKey("order-key")).thenReturn(Optional.of(payment));
        assertThat(useCase.execute("order-key")).isSameAs(payment);
        verify(payments).findByOrderKey("order-key");
    }

    @Test
    void 주문키에_해당하는_결제가_없으면_조회_오류다() {
        when(payments.findByOrderKey("missing-order")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.execute("missing-order")).isInstanceOf(CoreException.class).extracting("errorCode")
            .isEqualTo(ErrorCode.NOT_FOUND_DATA);
    }
}
