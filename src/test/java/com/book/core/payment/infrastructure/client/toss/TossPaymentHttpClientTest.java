package com.book.core.payment.infrastructure.client.toss;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class TossPaymentHttpClientTest {
    @Test
    void 실제_RestClient이_인증과_멱등키를_전송하고_응답을_읽는다() throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        final AtomicReference<String> body = new AtomicReference<>();
        final AtomicReference<String> authorization = new AtomicReference<>();
        final AtomicReference<String> idempotencyKey = new AtomicReference<>();
        server.createContext("/v1/payments", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            idempotencyKey.set(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
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
            withClient(server, "test_sk_wire", client -> {
                final var result = client.approve("test-key", "order-001", java.math.BigDecimal.TEN, "request-001");
                assertThat(result.approveCode()).isEqualTo("12345678");
                assertThat(authorization.get()).isEqualTo("Basic dGVzdF9za193aXJlOg==");
                assertThat(idempotencyKey.get()).isEqualTo("request-001");
                assertThat(body.get()).contains("\"orderId\":\"order-001\"", "\"amount\":10");
            });
        } finally {
            server.stop(0);
        }
    }

    @Test
    void 실제_HTTP에서_결제조회와_취소요청_인증_멱등키_본문을_검증한다() throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        final AtomicReference<String> findPath = new AtomicReference<>();
        final AtomicReference<String> findAuthorization = new AtomicReference<>();
        final AtomicReference<String> cancelPath = new AtomicReference<>();
        final AtomicReference<String> cancelAuthorization = new AtomicReference<>();
        final AtomicReference<String> cancelIdempotencyKey = new AtomicReference<>();
        final AtomicReference<String> cancelBody = new AtomicReference<>();
        server.createContext("/v1/payments/test-key/cancel", exchange -> {
            cancelPath.set(exchange.getRequestURI().getPath());
            cancelAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            cancelIdempotencyKey.set(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
            cancelBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            final byte[] response = """
                {"paymentKey":"test-key","orderId":"order-001","totalAmount":10,"balanceAmount":9,
                 "status":"PARTIAL_CANCELED","method":"카드","cancels":[{"transactionKey":"txn-001",
                 "cancelAmount":1,"refundableAmount":9,"cancelStatus":"DONE","canceledAt":"2026-10-20T10:00:00+09:00"}]}
                """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.createContext("/v1/payments/test-key", exchange -> {
            findPath.set(exchange.getRequestURI().getPath());
            findAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            final byte[] response = """
                {"paymentKey":"test-key","orderId":"order-001","totalAmount":10,"balanceAmount":10,
                 "status":"DONE","method":"카드","card":{"approveNo":"12345678"}}
                """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            withClient(server, "test_sk_wire", client -> {
                assertThat(client.find("test-key").status()).isEqualTo("DONE");
                assertThat(client.cancel("test-key", java.math.BigDecimal.ONE, "cancel-001")
                    .cancellations().getFirst().transactionKey()).isEqualTo("txn-001");
            });
            assertThat(findPath.get()).isEqualTo("/v1/payments/test-key");
            assertThat(findAuthorization.get()).isEqualTo("Basic dGVzdF9za193aXJlOg==");
            assertThat(cancelPath.get()).isEqualTo("/v1/payments/test-key/cancel");
            assertThat(cancelAuthorization.get()).isEqualTo("Basic dGVzdF9za193aXJlOg==");
            assertThat(cancelIdempotencyKey.get()).isEqualTo("cancel-001");
            assertThat(cancelBody.get()).contains("\"cancelReason\":\"고객 요청\"", "\"cancelAmount\":1")
                .doesNotContain("refundableAmount");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void 시크릿키가_없으면_인증_요청을_전송하지_않는다() throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        final AtomicInteger requests = new AtomicInteger();
        server.createContext("/v1/payments/test-key", exchange -> {
            requests.incrementAndGet();
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();
        try {
            withClient(server, "", client -> assertThatThrownBy(() -> client.find("test-key")).isInstanceOfSatisfying(CoreException.class,
                error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.PG_NOT_CONFIGURED)));
            assertThat(requests.get()).isZero();
        } finally {
            server.stop(0);
        }
    }

    @Test
    void HTTP_오류를_분류하고_응답본문을_노출하거나_재시도하지_않는다() throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        final AtomicInteger requests = new AtomicInteger();
        final AtomicReference<Integer> status = new AtomicReference<>(400);
        server.createContext("/v1/payments/confirm", exchange -> {
            requests.incrementAndGet();
            final byte[] body = "secret response body".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status.get(), body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            withClient(server, "test_sk_wire", client -> {
                assertThatThrownBy(() -> client.approve("test-key", "order-001", java.math.BigDecimal.TEN, "request-001"))
                    .isInstanceOfSatisfying(CoreException.class, error -> {
                        assertThat(error.errorCode()).isEqualTo(ErrorCode.PG_UNAVAILABLE);
                        assertThat(error.getMessage()).doesNotContain("secret response body");
                    });
                status.set(503);
                assertThatThrownBy(() -> client.approve("test-key", "order-001", java.math.BigDecimal.TEN, "request-002"))
                    .isInstanceOfSatisfying(CoreException.class, error -> {
                        assertThat(error.errorCode()).isEqualTo(ErrorCode.PG_UNAVAILABLE);
                        assertThat(error.getMessage()).doesNotContain("secret response body");
                    });
            });
            assertThat(requests.get()).isEqualTo(2);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void 문서화된_승인_카드거절만_PG_REJECTED로_분류한다() throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        final AtomicInteger requests = new AtomicInteger();
        server.createContext("/v1/payments", exchange -> {
            final int request = requests.getAndIncrement();
            final int status = request == 1 || request == 2 ? 403 : 400;
            final String code = switch (request) {
                case 0 -> "INVALID_REJECT_CARD";
                case 1 -> "REJECT_CARD_COMPANY";
                default -> "REJECT_CARD_PAYMENT";
            };
            final byte[] response = ("{\"code\":\"" + code + "\"}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            withClient(server, "test_sk_wire", client -> {
                for (int i = 0; i < 3; i++) {
                    assertThatThrownBy(() -> client.approve("test-key", "order-001", java.math.BigDecimal.TEN, "request-001"))
                        .isInstanceOfSatisfying(CoreException.class,
                            error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.PG_REJECTED));
                }
                assertThatThrownBy(() -> client.cancel("test-key", java.math.BigDecimal.ONE, "request-004"))
                    .isInstanceOfSatisfying(CoreException.class,
                        error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.PG_UNAVAILABLE));
            });
            assertThat(requests.get()).isEqualTo(4);
        } finally {
            server.stop(0);
        }
    }

    private static void withClient(final HttpServer server, final String secretKey, final ClientAction action) {
        new ApplicationContextRunner().withUserConfiguration(TossPaymentConfiguration.class)
            .withPropertyValues("payment.toss.base-url=http://127.0.0.1:" + server.getAddress().getPort(),
                "payment.toss.secret-key=" + secretKey)
            .run(context -> action.accept(new PaymentClientImpl(context.getBean(TossPaymentHttpClient.class))));
    }

    @FunctionalInterface
    private interface ClientAction {
        void accept(PaymentClientImpl client);
    }
}
