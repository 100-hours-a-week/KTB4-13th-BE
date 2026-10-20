package com.book.core.payment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.book.common.filter.RequestMdcFilter;
import com.book.core.order.application.usecase.GetPaymentOrderUseCase;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderStatus;
import com.book.core.payment.application.port.PaymentHistoryRepositoryPort;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.enums.PaymentState;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.test.util.ReflectionTestUtils;

class PaymentProcessingLoggingTest {
    private final PaymentRepositoryPort payments = mock(PaymentRepositoryPort.class);
    private final GetPaymentOrderUseCase orders = mock(GetPaymentOrderUseCase.class);
    private final PaymentHistoryRepositoryPort histories = mock(PaymentHistoryRepositoryPort.class);
    private final FailPaymentUseCase useCase = new FailPaymentUseCase(payments, orders, histories);
    private final Logger logger = (Logger) LoggerFactory.getLogger(FailPaymentUseCase.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private Level previousLevel;
    private boolean previousAdditive;
    private Map<String, String> previousMdc;

    @BeforeEach
    void captureLogs() {
        previousLevel = logger.getLevel();
        previousAdditive = logger.isAdditive();
        previousMdc = MDC.getCopyOfContextMap();
        logger.setLevel(Level.DEBUG);
        logger.setAdditive(false);
        appender.start();
        logger.addAppender(appender);
        MDC.put(RequestMdcFilter.REQUEST_ID_MDC_KEY, "payment-test-request");
        final Order order = Order.create(1L, "order", null);
        ReflectionTestUtils.setField(order, "id", 2L);
        final Payment payment =
            new Payment(1L, 2L, BigDecimal.TEN, -1L, BigDecimal.ZERO, BigDecimal.ZERO, 1L, BigDecimal.TEN, PaymentState.READY);
        ReflectionTestUtils.setField(payment, "id", 3L);
        when(orders.execute("order", OrderStatus.CREATED)).thenReturn(order);
        when(payments.findByOrderId(2L)).thenReturn(Optional.of(payment));
    }

    @AfterEach
    void restoreLogs() {
        logger.detachAppender(appender);
        logger.setLevel(previousLevel);
        logger.setAdditive(previousAdditive);
        appender.stop();
        MDC.clear();
        if (previousMdc != null) {
            MDC.setContextMap(previousMdc);
        }
    }

    @Test
    void 실패_이력_로그는_식별자와_추적값만_남기고_외부_원문은_노출하지_않는다() {
        useCase.execute("order", "private-external-code", "private-external-message");

        assertThat(appender.list).extracting(ILoggingEvent::getLevel).containsExactly(Level.DEBUG, Level.INFO);
        assertThat(appender.list).allSatisfy((final ILoggingEvent event) -> assertThat(event.getFormattedMessage())
            .contains("paymentId=3", "orderId=2", "requestId=payment-test-request")
            .doesNotContain("traceId=", "private-external-code", "private-external-message", "externalPaymentKey=", "amount="));
    }

    @Test
    void 이력_저장이_실패하면_처리_종료_INFO를_남기지_않는다() {
        doThrow(new IllegalStateException("test database failure")).when(histories).save(any());

        assertThatThrownBy(() -> useCase.execute("order", "private-code", "private-message")).isInstanceOf(IllegalStateException.class);
        assertThat(appender.list).extracting(ILoggingEvent::getLevel).containsExactly(Level.DEBUG);
    }
}
