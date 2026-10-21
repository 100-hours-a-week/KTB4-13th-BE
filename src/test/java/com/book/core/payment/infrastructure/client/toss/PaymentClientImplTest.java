package com.book.core.payment.infrastructure.client.toss;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.payment.domain.enums.PaymentMethod;
import com.book.core.payment.application.result.PgPaymentResult;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.RestClientException;

class PaymentClientImplTest {
    private final TossPaymentHttpClient httpClient = mock(TossPaymentHttpClient.class);
    private final PaymentClientImpl client = new PaymentClientImpl(httpClient);

    @Test
    void 승인_입력과_멱등키를_전달하고_카드_응답을_변환한다() {
        when(httpClient.approve(eq("operation"), any())).thenReturn(response("카드", new TossPaymentResponse.Card("12345678")));
        final var result = client.approve("test-key", "order-001", BigDecimal.TEN, "operation");
        final var request = ArgumentCaptor.forClass(TossPaymentHttpClient.ApproveRequest.class);
        verify(httpClient).approve(eq("operation"), request.capture());
        assertThat(request.getValue().orderId()).isEqualTo("order-001");
        assertThat(request.getValue().paymentKey()).isEqualTo("test-key");
        assertThat(request.getValue().amount()).isEqualByComparingTo("10");
        assertThat(result.method()).isEqualTo(PaymentMethod.CARD);
        assertThat(result.approveCode()).isEqualTo("12345678");
        assertThat(result.toString()).doesNotContain("test-key");
    }

    @Test
    void 카드가_없는_간편결제도_결제수단을_변환한다() {
        when(httpClient.find("test-key")).thenReturn(response("간편결제", null));
        final PgPaymentResult result = client.find("test-key");
        assertThat(result.method()).isEqualTo(PaymentMethod.EASY_PAY);
        assertThat(result.approveCode()).isNull();
    }

    @Test
    void 카드_기반_간편결제는_카드_승인번호를_보존한다() {
        when(httpClient.find("test-key")).thenReturn(response("간편결제", new TossPaymentResponse.Card("12345678")));
        final PgPaymentResult result = client.find("test-key");
        assertThat(result.method()).isEqualTo(PaymentMethod.EASY_PAY);
        assertThat(result.approveCode()).isEqualTo("12345678");
    }

    @Test
    void 취소_사유와_취소금액을_전달한다() {
        when(httpClient.cancel(eq("test-key"), eq("operation"), any())).thenReturn(response("카드", null));
        client.cancel("test-key", BigDecimal.ONE, "operation");
        final var request = ArgumentCaptor.forClass(TossPaymentHttpClient.CancelRequest.class);
        verify(httpClient).cancel(eq("test-key"), eq("operation"), request.capture());
        assertThat(request.getValue().cancelReason()).isEqualTo("고객 요청");
        assertThat(request.getValue().cancelAmount()).isEqualByComparingTo("1");
    }

    @Test
    void 네트워크_실패는_민감정보_없는_미확정_오류로_바꾼다() {
        when(httpClient.find("test-key")).thenThrow(new RestClientException("secret response"));
        assertThatThrownBy(() -> client.find("test-key")).isInstanceOf(CoreException.class)
            .satisfies(error -> assertThat(((CoreException) error).errorCode()).isEqualTo(ErrorCode.PG_UNAVAILABLE));
    }

    @Test
    void 필수_응답이_없으면_거절한다() {
        when(httpClient.find("test-key")).thenReturn(null);
        assertThatThrownBy(() -> client.find("test-key")).isInstanceOf(CoreException.class);
    }

    private static TossPaymentResponse response(final String method, final TossPaymentResponse.Card card) {
        return new TossPaymentResponse("test-key", "order-001", BigDecimal.TEN, BigDecimal.TEN, "DONE", method, card, List.of());
    }
}
