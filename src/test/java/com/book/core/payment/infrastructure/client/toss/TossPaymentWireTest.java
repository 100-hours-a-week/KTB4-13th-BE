package com.book.core.payment.infrastructure.client.toss;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;

class TossPaymentWireTest {
    @Test
    void 실제_Feign이_인증과_멱등헤더와_JSON을_전송하고_응답을_읽는다() throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        final AtomicReference<String> body = new AtomicReference<>();
        final AtomicReference<String> auth = new AtomicReference<>();
        final AtomicReference<String> key = new AtomicReference<>();
        server.createContext("/v1/payments/confirm", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            key.set(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
            final byte[] response = """
                {"paymentKey":"test-key","orderId":"order-001","totalAmount":10,"balanceAmount":10,
                 "status":"DONE","method":"카드","card":{"approveNo":"12345678"},"unknown":"ignored"}
                """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(FeignAutoConfiguration.class))
                .withUserConfiguration(TossPaymentConfiguration.class)
                .withPropertyValues("payment.toss.base-url=http://127.0.0.1:" + server.getAddress().getPort(),
                    "payment.toss.secret-key=test_sk_wire")
                .run(context -> {
                    final var client = new PaymentClientImpl(context.getBean(TossPaymentFeignClient.class));
                    final var result = client.approve("test-key", "order-001", java.math.BigDecimal.TEN, "request-001");
                    assertThat(result.approveCode()).isEqualTo("12345678");
                    assertThat(auth.get()).isEqualTo("Basic dGVzdF9za193aXJlOg==");
                    assertThat(key.get()).isEqualTo("request-001");
                    assertThat(body.get()).contains("\"orderId\":\"order-001\"", "\"amount\":10");
                });
        } finally {
            server.stop(0);
        }
    }

    @Test
    void 시크릿키가_없으면_인증_요청을_전송하지_않는다() {
        final var interceptor = new TossPaymentFeignConfiguration().tossAuthentication("");
        assertThatThrownBy(() -> interceptor.apply(new feign.RequestTemplate())).isInstanceOfSatisfying(CoreException.class,
            error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.PG_NOT_CONFIGURED));
    }
}
