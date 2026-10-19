package com.book.core.payment.infrastructure.client.toss;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TossPaymentErrorDecoderTest {
    private static final String APPROVE_METHOD = "TossPaymentFeignClient#approve(String,ApproveRequest)";
    private static final String CANCEL_METHOD = "TossPaymentFeignClient#cancel(String,String,CancelRequest)";

    private final TossPaymentFeignConfiguration configuration = new TossPaymentFeignConfiguration();

    @Test
    void 승인_요청은_문서화된_카드_거절_상태와_오류코드만_확정_거절로_분류한다() {
        assertThat(decode(APPROVE_METHOD, 400, "{\"code\":\"INVALID_REJECT_CARD\"}").errorCode()).isEqualTo(ErrorCode.PG_REJECTED);
        assertThat(decode(APPROVE_METHOD, 403, "{\"code\":\"REJECT_CARD_COMPANY\"}").errorCode()).isEqualTo(ErrorCode.PG_REJECTED);
        assertThat(decode(APPROVE_METHOD, 403, "{\"code\":\"REJECT_CARD_PAYMENT\"}").errorCode()).isEqualTo(ErrorCode.PG_REJECTED);
    }

    @Test
    void 취소_미확정_또는_미문서화된_오류는_재시도_복구_대기로_남긴다() {
        assertThat(decode(CANCEL_METHOD, 403, "{\"code\":\"REJECT_CARD_COMPANY\"}").errorCode()).isEqualTo(ErrorCode.PG_UNAVAILABLE);
        assertThat(decode(APPROVE_METHOD, 400, "{\"code\":\"REJECT_CARD_PAYMENT\"}").errorCode()).isEqualTo(ErrorCode.PG_UNAVAILABLE);
        for (final int status : new int[] {400, 403, 408, 409, 429, 500}) {
            assertThat(decode(APPROVE_METHOD, status, "{\"code\":\"UNKNOWN\"}").errorCode()).isEqualTo(ErrorCode.PG_UNAVAILABLE);
        }
    }

    private CoreException decode(final String method, final int status, final String body) {
        final var request =
            feign.Request.create(feign.Request.HttpMethod.POST, "http://localhost", Map.of(), null, StandardCharsets.UTF_8, null);
        final var response = feign.Response.builder().status(status).reason("test").request(request).headers(Map.of())
            .body(body, StandardCharsets.UTF_8).build();
        return (CoreException) configuration.tossErrorDecoder().decode(method, response);
    }
}
