package com.book.core.payment.infrastructure.client.toss;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cancel.application.command.CancelCommand;
import com.book.core.cancel.application.command.PartialCancelCommand;
import com.book.core.cancel.application.port.CancelBalanceRepositoryPort;
import com.book.core.cancel.application.service.CancelService;
import com.book.core.cancel.application.usecase.CancelPaymentUseCase;
import com.book.core.cancel.domain.CancelAction;
import com.book.core.cancel.domain.CancelBalance;
import com.book.core.cancel.domain.PartialCancelAction;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderStatus;
import com.book.core.payment.application.command.ApprovePaymentCommand;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.application.service.PaymentService;
import com.book.core.payment.application.usecase.CompletePaymentUseCase;
import com.book.core.payment.application.usecase.PostProcessPaymentUseCase;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.enums.PaymentMethod;
import com.book.core.payment.domain.enums.PaymentState;
import com.book.core.point.application.port.PointBalanceRepositoryPort;
import com.book.core.point.domain.PointBalance;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@ActiveProfiles({"test", "mysql-test"})
@Testcontainers
@Tag("integration")
class TossPaymentRecoveryIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");
    private static final AtomicInteger users = new AtomicInteger(60000);
    private static final Map<String, GatewayPayment> gateway = new ConcurrentHashMap<>();
    private static final HttpServer server = startServer();

    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("payment.toss.base-url", () -> "http://127.0.0.1:" + server.getAddress().getPort());
        registry.add("payment.toss.secret-key", () -> "test_sk_wire");
    }

    @AfterAll
    static void closeServer() {
        server.stop(0);
    }

    @Autowired
    private com.book.core.order.application.usecase.CancelOrderUseCase cancelOrder;
    @Autowired
    private PaymentService payments;
    @Autowired
    private CancelService cancels;
    @Autowired
    private PaymentRepositoryPort paymentRepository;
    @Autowired
    private OrderRepositoryPort orders;
    @Autowired
    private CancelBalanceRepositoryPort balances;
    @Autowired
    private PointBalanceRepositoryPort points;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PlatformTransactionManager transactions;
    @MockitoSpyBean
    private CompletePaymentUseCase completePayment;
    @MockitoSpyBean
    private CancelPaymentUseCase completeCancel;
    @MockitoSpyBean
    private PostProcessPaymentUseCase postProcessPayment;

    @Test
    void 승인_성공후_DB실패는_요청을_보존하고_조회로_내부_반영을_복구한다() {
        final Fixture fixture = fixture(false, 1);
        final var command = new ApprovePaymentCommand(fixture.order().key(), fixture.key(), fixture.pg().total);
        doThrow(new CoreException(ErrorCode.STORAGE_FAILURE)).doCallRealMethod().when(completePayment).execute(eq(fixture.order().key()),
            any());
        assertThatThrownBy(() -> payments.success(command)).isInstanceOf(CoreException.class);
        assertThat(fixture.pg().approvals).isEqualTo(1);
        assertThat(pending(fixture)).isEqualTo(1L);
        assertThatThrownBy(() -> cancelOrder
            .execute(new com.book.core.order.application.command.CancelOrderCommand(fixture.order().userId(), fixture.order().key())))
            .isInstanceOfSatisfying(CoreException.class, error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.PG_PENDING));
        assertThat(orders.findActiveOrderByKey(fixture.order().key()).orElseThrow().status()).isEqualTo(OrderStatus.CREATED);
        final Long result = payments.success(command);
        assertThat(payments.success(command)).isEqualTo(result);
        assertThat(fixture.pg().approvals).isEqualTo(1);
        assertThat(pending(fixture)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from transaction_history where order_id = ? and type = 'PAYMENT'", Long.class,
            fixture.order().id())).isEqualTo(1L);
        assertThat(points.findByUserId(fixture.order().userId()).orElseThrow().balance()).isEqualByComparingTo("22000");
        verify(postProcessPayment, times(1)).execute(result, fixture.order().key());
    }

    @Test
    void 전체취소_성공후_DB실패는_같은_PG키로_취소결과를_복구한다() {
        final Fixture fixture = fixture(true, 1);
        final var command = new CancelCommand(fixture.order().userId(), new CancelAction(fixture.order().key()), "full-request");
        doThrow(new CoreException(ErrorCode.STORAGE_FAILURE)).doCallRealMethod().when(completeCancel).execute(any(), any());
        assertThatThrownBy(() -> cancels.cancel(command)).isInstanceOf(CoreException.class);
        assertThat(fixture.pg().refunds).isEqualTo(1);
        assertThat(pending(fixture)).isEqualTo(1L);
        final Long id = cancels.cancel(command);
        assertThat(cancels.cancel(command)).isEqualTo(id);
        assertThat(fixture.pg().refunds).isEqualTo(1);
        assertThat(pending(fixture)).isZero();
        assertThat(jdbc.queryForObject("select external_cancel_key from `cancel` where id = ?", String.class, id)).isEqualTo("cancel-1");
    }

    @Test
    void 취소_응답유실은_내부를_롤백하고_같은_키의_응답으로_재개한다() {
        final Fixture fixture = fixture(true, 3);
        fixture.pg().dropResponse = true;
        final var command = partial(fixture, "partial-request");
        assertThatThrownBy(() -> cancels.partialCancel(command)).isInstanceOf(CoreException.class);
        assertThat(fixture.pg().refunds).isEqualTo(1);
        assertThat(pending(fixture)).isEqualTo(1L);
        assertThat(canceledQuantity(fixture)).isZero();
        cancels.partialCancel(command);
        assertThat(fixture.pg().refunds).isEqualTo(1);
        assertThat(canceledQuantity(fixture)).isEqualTo(1L);
    }

    @Test
    void 동일_요청키의_동시_부분취소는_한번만_환불한다() throws Exception {
        final Fixture fixture = fixture(true, 3);
        final var command = partial(fixture, "same-request");
        try (final var pool = Executors.newFixedThreadPool(2)) {
            final var first = pool.submit(() -> cancels.partialCancel(command));
            final var second = pool.submit(() -> cancels.partialCancel(command));
            assertThat(first.get(20, TimeUnit.SECONDS)).isEqualTo(second.get(20, TimeUnit.SECONDS));
        }
        assertThat(fixture.pg().refunds).isEqualTo(1);
        assertThat(canceledQuantity(fixture)).isEqualTo(1L);
    }

    @Test
    void 서로_다른_요청키가_마지막_취소수량을_경쟁해도_한번만_환불한다() throws Exception {
        final Fixture fixture = fixture(true, 1);
        final var first = partial(fixture, "different-first");
        final var second = partial(fixture, "different-second");
        try (final var pool = Executors.newFixedThreadPool(2)) {
            final var firstAttempt = pool.submit(() -> attempt(first));
            final var secondAttempt = pool.submit(() -> attempt(second));
            final List<CancelAttempt> attempts = List.of(firstAttempt.get(20, TimeUnit.SECONDS), secondAttempt.get(20, TimeUnit.SECONDS));
            assertThat(attempts.stream().filter(CancelAttempt::succeeded)).hasSize(1);
            assertThat(attempts.stream().filter(result -> !result.succeeded()).map(CancelAttempt::errorCode).toList()).singleElement()
                .isIn(ErrorCode.PG_PENDING, ErrorCode.INVALID_REQUEST);
        }
        assertThat(fixture.pg().refunds).isEqualTo(1);
        assertThat(canceledQuantity(fixture)).isEqualTo(1L);
    }

    @Test
    void 다른_키의_정상_반복취소는_허용하고_같은_키의_입력변경은_거절한다() {
        final Fixture fixture = fixture(true, 3);
        cancels.partialCancel(partial(fixture, "first"));
        assertThatThrownBy(() -> cancels.partialCancel(new PartialCancelCommand(fixture.order().userId(),
            new PartialCancelAction(fixture.order().key(), itemId(fixture), 2), "first")))
            .isInstanceOfSatisfying(CoreException.class, error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.PG_REQUEST_CONFLICT));
        cancels.partialCancel(partial(fixture, "second"));
        assertThat(fixture.pg().refunds).isEqualTo(2);
        assertThat(canceledQuantity(fixture)).isEqualTo(2L);
    }

    @Test
    void 미확정_요청은_새로운_키와_만료후_재전송을_차단한다() {
        final Fixture fixture = fixture(true, 3);
        fixture.pg().dropResponse = true;
        final var command = partial(fixture, "pending");
        assertThatThrownBy(() -> cancels.partialCancel(command)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> cancels.partialCancel(partial(fixture, "other"))).isInstanceOfSatisfying(CoreException.class,
            error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.PG_PENDING));
        jdbc.update("update pg_operation set requested_at = DATE_SUB(UTC_TIMESTAMP(), INTERVAL 15 DAY) where order_key = ?",
            fixture.order().key());
        assertThatThrownBy(() -> cancels.partialCancel(command)).isInstanceOfSatisfying(CoreException.class,
            error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.PG_REVIEW_REQUIRED));
        assertThat(fixture.pg().refunds).isEqualTo(1);
    }

    @Test
    void 만료된_PG승인은_거절로_기록하고_새_결제키의_승인을_허용한다() {
        final Fixture fixture = fixture(false, 1);
        fixture.pg().terminalState = "EXPIRED";
        final var failed = new ApprovePaymentCommand(fixture.order().key(), fixture.key(), fixture.pg().total);
        assertThatThrownBy(() -> payments.success(failed)).isInstanceOfSatisfying(CoreException.class,
            error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.PG_REJECTED));
        assertThat(pending(fixture)).isZero();
        final String newKey = "pg-" + UUID.randomUUID();
        final GatewayPayment replacement = new GatewayPayment(fixture.order().key(), newKey, fixture.pg().total, false);
        gateway.put(newKey, replacement);
        payments.success(new ApprovePaymentCommand(fixture.order().key(), newKey, fixture.pg().total));
        assertThat(replacement.approvals).isEqualTo(1);
        assertThat(fixture.pg().approvals).isZero();
    }

    private long pending(final Fixture fixture) {
        return jdbc.queryForObject("select count(*) from pg_operation where order_key = ? and completed = false and rejected = false",
            Long.class, fixture.order().key());
    }

    private long canceledQuantity(final Fixture fixture) {
        return jdbc.queryForObject("select canceled_quantity from order_item where order_id = ?", Long.class, fixture.order().id());
    }

    private Long itemId(final Fixture fixture) {
        return jdbc.queryForObject("select id from order_item where order_id = ?", Long.class, fixture.order().id());
    }

    private PartialCancelCommand partial(final Fixture fixture, final String key) {
        return new PartialCancelCommand(fixture.order().userId(), new PartialCancelAction(fixture.order().key(), itemId(fixture), 1), key);
    }

    private CancelAttempt attempt(final PartialCancelCommand command) {
        try {
            return new CancelAttempt(cancels.partialCancel(command), null);
        } catch (final CoreException exception) {
            return new CancelAttempt(null, exception.errorCode());
        }
    }

    private Fixture fixture(final boolean paid, final int quantity) {
        final long userId = users.incrementAndGet();
        final long productId = 2000000 + userId;
        jdbc.update("INSERT INTO books (id, title, author, publisher, category, published_at) "
            + "VALUES (?, 'PG 테스트', '저자', '출판사', '소설', '2026-01-01')", productId);
        jdbc.update("INSERT INTO products (id, book_id, name, sale_price, discounted_price, cost_price, stock_quantity) "
            + "VALUES (?, ?, 'PG 상품', 10000, 10000, 0, 10)", productId, productId);
        return new TransactionTemplate(transactions).execute(status -> {
            final Order order = Order.create(userId, "pg-order-" + UUID.randomUUID(), null);
            order.addItem(productId, "PG 상품", "", "저자", new BigDecimal("10000"), new BigDecimal("10000"), quantity);
            if (paid) {
                order.paid();
            }
            orders.save(order);
            final String key = "pg-" + UUID.randomUUID();
            final GatewayPayment pg = new GatewayPayment(order.key(), key, order.totalPrice(), paid);
            gateway.put(key, pg);
            final Payment payment = new Payment(userId, order.id(), order.totalPrice(), -1L, BigDecimal.ZERO, BigDecimal.ZERO, userId,
                order.totalPrice(), PaymentState.READY);
            if (paid) {
                payment.success(key, PaymentMethod.CARD, "12345678");
            }
            paymentRepository.save(payment);
            points.save(new PointBalance(null, userId, new BigDecimal("20000")));
            if (paid) {
                balances.save(new CancelBalance(order.id(), order.totalPrice(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO));
            }
            return new Fixture(order, key, pg);
        });
    }

    private record Fixture(Order order, String key, GatewayPayment pg) {
    }

    private record CancelAttempt(Long resultId, ErrorCode errorCode) {
        private boolean succeeded() {
            return resultId != null;
        }
    }

    private static HttpServer startServer() {
        try {
            final HttpServer http = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            http.createContext("/v1/payments", TossPaymentRecoveryIntegrationTest::handle);
            http.start();
            return http;
        } catch (final IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void handle(final HttpExchange exchange) throws IOException {
        if (!"Basic dGVzdF9za193aXJlOg==".equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
            respond(exchange, 401, "{}");
            return;
        }
        final String path = exchange.getRequestURI().getPath();
        final var mapper = JsonMapper.builder().build();
        if (path.endsWith("/confirm")) {
            final var input = mapper.readTree(exchange.getRequestBody());
            final GatewayPayment pg = gateway.get(input.path("paymentKey").asText());
            pg.paid = true;
            pg.approvals++;
            respond(exchange, 200, pg.json());
            return;
        }
        final String key = path.split("/")[3];
        final GatewayPayment pg = gateway.get(key);
        if (pg == null) {
            respond(exchange, 404, "{}");
            return;
        }
        if (path.endsWith("/cancel")) {
            final String idempotencyKey = exchange.getRequestHeaders().getFirst("Idempotency-Key");
            if (pg.cached.containsKey(idempotencyKey)) {
                respond(exchange, 200, pg.cached.get(idempotencyKey));
                return;
            }
            final var input = mapper.readTree(exchange.getRequestBody());
            final BigDecimal amount = input.path("cancelAmount").decimalValue();
            if (amount.signum() <= 0 || amount.compareTo(pg.balance) > 0) {
                respond(exchange, 400, "{}");
                return;
            }
            pg.balance = pg.balance.subtract(amount);
            pg.refunds++;
            pg.cancellations.add("{\"transactionKey\":\"cancel-" + pg.refunds + "\",\"cancelAmount\":" + amount + ",\"refundableAmount\":"
                + pg.balance + ",\"cancelStatus\":\"DONE\",\"canceledAt\":\"" + OffsetDateTime.now() + "\"}");
            final String result = pg.json();
            pg.cached.put(idempotencyKey, result);
            if (pg.dropResponse) {
                pg.dropResponse = false;
                exchange.close();
                return;
            }
            respond(exchange, 200, result);
            return;
        }
        respond(exchange, 200, pg.json());
    }

    private static void respond(final HttpExchange exchange, final int status, final String json) throws IOException {
        final byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static final class GatewayPayment {
        private final String orderKey;
        private final String key;
        private final BigDecimal total;
        private BigDecimal balance;
        private boolean paid;
        private String terminalState;
        private int approvals;
        private int refunds;
        private volatile boolean dropResponse;
        private final Map<String, String> cached = new ConcurrentHashMap<>();
        private final ArrayList<String> cancellations = new ArrayList<>();

        private GatewayPayment(final String orderKey, final String key, final BigDecimal total, final boolean paid) {
            this.orderKey = orderKey;
            this.key = key;
            this.total = total;
            this.balance = total;
            this.paid = paid;
        }

        private String json() {
            String state = "IN_PROGRESS";
            if (paid) {
                state = "DONE";
            }
            if (refunds > 0) {
                state = "PARTIAL_CANCELED";
            }
            if (balance.signum() == 0) {
                state = "CANCELED";
            }
            if (terminalState != null) {
                state = terminalState;
            }
            return "{\"paymentKey\":\"" + key + "\",\"orderId\":\"" + orderKey + "\",\"totalAmount\":" + total + ",\"balanceAmount\":"
                + balance + ",\"status\":\"" + state + "\",\"method\":\"카드\",\"card\":{\"approveNo\":\"12345678\"},\"cancels\":["
                + String.join(",", cancellations) + "]}";
        }
    }
}
