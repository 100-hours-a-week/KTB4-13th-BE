package com.book.core.order.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.anyString;

import com.book.core.notification.application.command.LowStockNotificationEvent;
import com.book.core.notification.application.usecase.CreateLowStockNotificationUseCase;
import com.book.core.notification.application.usecase.CreateNotificationUseCase;
import com.book.core.order.application.command.CreateOrderCommand;
import com.book.core.order.application.command.CreateOrderItemCommand;
import com.book.core.order.application.usecase.CreateOrderUseCase;
import com.book.core.product.application.command.ProductQuantitiesCommand;
import com.book.core.product.application.result.LowStockInventoryEntry;
import com.book.core.product.application.usecase.ReceiveProductRestockUseCase;
import com.book.core.product.application.usecase.ReleaseProductInventoryUseCase;
import com.book.core.product.application.usecase.ReserveProductInventoryUseCase;
import com.book.core.product.application.usecase.FindLowStockNotificationCandidatesUseCase;
import com.book.core.product.application.usecase.RecordProductViewUseCase;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest(properties = "app.low-stock-notification.mode=V3")
@ActiveProfiles({"test", "mysql-test"})
@Testcontainers
@Tag("integration")
@Tag("mysql-migration")
class V3LowStockNotificationIntegrationTest {
    private static final AtomicLong NEXT_ID = new AtomicLong(5_100_000L);

    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    OrderService orderService;

    @Autowired
    ReserveProductInventoryUseCase reserve;

    @Autowired
    ReleaseProductInventoryUseCase release;

    @Autowired
    ReceiveProductRestockUseCase receiveRestock;

    @Autowired
    CreateLowStockNotificationUseCase createLowStockNotification;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    Clock clock;

    @Autowired
    LowStockNotificationMode mode;

    @Autowired
    FindLowStockNotificationCandidatesUseCase findCandidates;

    @Autowired
    RecordProductViewUseCase recordProductView;

    @MockitoSpyBean
    CreateNotificationUseCase createNotification;

    @MockitoSpyBean
    CreateOrderUseCase createOrder;

    @Test
    void V3_주문_커밋_후_조회_후보에게만_알림을_저장한다() throws Exception {
        final long orderingUserId = insertUser();
        final long candidateId = insertUser();
        final long onceViewedId = insertUser();
        final long productId = insertProduct(6, orderingUserId);
        viewTwice(orderingUserId, productId);
        viewTwice(candidateId, productId);
        insertView(onceViewedId, productId);

        assertThat(mode).isEqualTo(LowStockNotificationMode.V3);
        assertThat(findCandidates.execute(productId, LocalDateTime.now(clock))).containsExactly(orderingUserId, candidateId);

        orderService.createOrder(new CreateOrderCommand(orderingUserId, List.of(new CreateOrderItemCommand(productId, 1))));

        assertThat(jdbc.queryForObject("SELECT reserved_quantity FROM product_inventory WHERE product_id = ?", Integer.class, productId))
            .isEqualTo(1);

        awaitNotificationCount(candidateId, 1);
        assertThat(notificationCount(orderingUserId)).isZero();
        assertThat(notificationCount(onceViewedId)).isZero();
        assertThat(jdbc.queryForObject("SELECT title FROM notifications WHERE user_id = ?", String.class, candidateId))
            .isEqualTo("조회한 상품 품절 임박");
    }

    @Test
    void 후보가_없으면_알림을_저장하지_않는다() throws Exception {
        final long orderingUserId = insertUser();
        final long productId = insertProduct(6, orderingUserId);

        orderService.createOrder(new CreateOrderCommand(orderingUserId, List.of(new CreateOrderItemCommand(productId, 1))));

        Thread.sleep(150);
        assertThat(notificationCount(orderingUserId)).isZero();
    }

    @Test
    void 주문_저장이_실패하면_예약이_롤백되고_알림_작업이_생성되지_않는다() throws Exception {
        final long orderingUserId = insertUser();
        final long candidateId = insertUser();
        final long productId = insertProduct(6, orderingUserId);
        viewTwice(candidateId, productId);
        doThrow(new IllegalStateException("주문 저장 실패")).when(createOrder).execute(any());

        assertThatThrownBy(
            () -> orderService.createOrder(new CreateOrderCommand(orderingUserId, List.of(new CreateOrderItemCommand(productId, 1)))))
            .isInstanceOf(IllegalStateException.class);

        assertThat(jdbc.queryForObject("SELECT reserved_quantity FROM product_inventory WHERE product_id = ?", Integer.class, productId))
            .isZero();
        Thread.sleep(150);
        assertThat(notificationCount(candidateId)).isZero();
    }

    @Test
    void 재고_회복과_신규_입고는_이전_이벤트를_무효화한다() {
        final long candidateId = insertUser();
        final long recoveredProductId = insertProduct(6);
        final LowStockNotificationEvent recoveredEvent = entryEvent(recoveredProductId, candidateId);
        release.execute(one(recoveredProductId));

        assertThat(createLowStockNotification.execute(recoveredEvent, candidateId)).isFalse();

        final long restockedProductId = insertProduct(6);
        final LowStockInventoryEntry restockedEntry = reserve
            .executeWithLowStockNotificationEntries(new ProductQuantitiesCommand(Map.of(restockedProductId, 2)), 5).get(restockedProductId);
        final LowStockNotificationEvent restockedEvent = new LowStockNotificationEvent(restockedProductId, candidateId, "상품",
            restockedEntry.state().lowStockNotificationVersion(), restockedEntry.referenceTime());
        jdbc.update("INSERT INTO product_restocks (product_id, quantity, status) VALUES (?, 1, 'PENDING')", restockedProductId);
        final Long restockId = jdbc.queryForObject("SELECT id FROM product_restocks WHERE product_id = ?", Long.class, restockedProductId);
        assertThat(receiveRestock.execute(restockId, LocalDateTime.now(clock))).isTrue();

        assertThat(jdbc.queryForObject("SELECT on_hand_quantity - reserved_quantity FROM product_inventory WHERE product_id = ?",
            Integer.class, restockedProductId)).isEqualTo(5);
        assertThat(createLowStockNotification.execute(restockedEvent, candidateId)).isFalse();
        assertThat(notificationCount(candidateId)).isZero();
    }

    @Test
    void 추가_주문으로_재고가_감소해도_같은_버전의_이벤트는_유효하다() {
        final long candidateId = insertUser();
        final long productId = insertProduct(6);
        final LowStockNotificationEvent event = entryEvent(productId, candidateId);
        reserve.executeWithLowStockNotificationEntries(one(productId), 5);

        assertThat(createLowStockNotification.execute(event, candidateId)).isTrue();
        assertThat(jdbc.queryForObject("SELECT body FROM notifications WHERE user_id = ?", String.class, candidateId))
            .isEqualTo("조회하신 '상품'의 재고가 4권 남았어요.");
    }

    @Test
    void 같은_사용자와_상품은_하루에_한_건만_저장한다() {
        final long candidateId = insertUser();
        final long productId = insertProduct(6);
        final LowStockNotificationEvent event = entryEvent(productId, candidateId);

        assertThat(createLowStockNotification.execute(event, candidateId)).isTrue();
        assertThat(createLowStockNotification.execute(event, candidateId)).isTrue();

        assertThat(notificationCount(candidateId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT event_key FROM notifications WHERE user_id = ?", String.class, candidateId))
            .isEqualTo("low-stock:v3:" + candidateId + ":" + productId + ":" + LocalDate.now(clock));
    }

    @Test
    void 다른_상품과_다른_사용자는_각각_알림을_받는다() {
        final long firstUserId = insertUser();
        final long secondUserId = insertUser();
        final long firstProductId = insertProduct(6);
        final long secondProductId = insertProduct(6);
        final LowStockNotificationEvent firstEvent = entryEvent(firstProductId, firstUserId);
        final LowStockNotificationEvent secondEvent = entryEvent(secondProductId, firstUserId);

        assertThat(createLowStockNotification.execute(firstEvent, firstUserId)).isTrue();
        assertThat(createLowStockNotification.execute(firstEvent, secondUserId)).isTrue();
        assertThat(createLowStockNotification.execute(secondEvent, firstUserId)).isTrue();

        assertThat(notificationCount(firstUserId)).isEqualTo(2);
        assertThat(notificationCount(secondUserId)).isEqualTo(1);
    }

    @Test
    void 중복_이후에도_다른_수신자의_알림을_저장한다() {
        final long firstUserId = insertUser();
        final long secondUserId = insertUser();
        final long productId = insertProduct(6);
        final LowStockNotificationEvent event = entryEvent(productId, firstUserId);

        assertThat(createLowStockNotification.execute(event, firstUserId)).isTrue();
        assertThat(createLowStockNotification.execute(event, firstUserId)).isTrue();
        assertThat(createLowStockNotification.execute(event, secondUserId)).isTrue();

        assertThat(notificationCount(firstUserId)).isEqualTo(1);
        assertThat(notificationCount(secondUserId)).isEqualTo(1);
    }

    @Test
    void 동시에_같은_수신자에게_저장해도_한_건만_생성한다() throws Exception {
        final long candidateId = insertUser();
        final long productId = insertProduct(6);
        final LowStockNotificationEvent event = entryEvent(productId, candidateId);
        final CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(4)) {
            final List<Future<Boolean>> results = java.util.stream.IntStream.range(0, 4).mapToObj(index -> executor.submit(() -> {
                start.await();
                return createLowStockNotification.execute(event, candidateId);
            })).toList();
            start.countDown();
            for (final Future<Boolean> result : results) {
                assertThat(result.get(10, TimeUnit.SECONDS)).isTrue();
            }
        }
        assertThat(notificationCount(candidateId)).isEqualTo(1);
    }

    @Test
    void 알림_INSERT_전까지_재고_잠금을_유지한다() throws Exception {
        final long candidateId = insertUser();
        final long productId = insertProduct(6);
        final LowStockNotificationEvent event = entryEvent(productId, candidateId);
        final CountDownLatch inserting = new CountDownLatch(1);
        final CountDownLatch finishInsert = new CountDownLatch(1);
        doAnswer(invocation -> {
            inserting.countDown();
            assertThat(finishInsert.await(5, TimeUnit.SECONDS)).isTrue();
            return invocation.callRealMethod();
        }).when(createNotification).executeIfEventKeyAbsent(any(), anyString());

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            final Future<Boolean> notification = executor.submit(() -> createLowStockNotification.execute(event, candidateId));
            assertThat(inserting.await(5, TimeUnit.SECONDS)).isTrue();
            final Future<?> recovery = executor.submit(() -> release.execute(one(productId)));
            Thread.sleep(150);
            assertThat(recovery.isDone()).isFalse();

            finishInsert.countDown();
            assertThat(notification.get()).isTrue();
            recovery.get();
        } finally {
            finishInsert.countDown();
        }
        assertThat(notificationCount(candidateId)).isEqualTo(1);
        assertThat(
            jdbc.queryForObject("SELECT low_stock_notification_version FROM product_inventory WHERE product_id = ?", Long.class, productId))
            .isEqualTo(1);
    }

    private LowStockNotificationEvent entryEvent(final long productId, final long orderUserId) {
        final LowStockInventoryEntry entry = reserve.executeWithLowStockNotificationEntries(one(productId), 5).get(productId);
        return new LowStockNotificationEvent(productId, orderUserId, "상품", entry.state().lowStockNotificationVersion(),
            entry.referenceTime());
    }

    private ProductQuantitiesCommand one(final long productId) {
        return new ProductQuantitiesCommand(Map.of(productId, 1));
    }

    private long insertUser() {
        final long userId = NEXT_ID.incrementAndGet();
        jdbc.update("INSERT INTO users (id, nickname) VALUES (?, ?)", userId, "v3" + userId);
        jdbc.update("INSERT INTO carts (user_id) VALUES (?)", userId);
        return userId;
    }

    private long insertProduct(final int onHandQuantity, final long... cartUserIds) {
        final long productId = NEXT_ID.incrementAndGet();
        jdbc.update(
            "INSERT INTO books (id, title, author, publisher, category, published_at) VALUES (?, '책', '작가', '출판사', '소설', '2026-01-01')",
            productId);
        jdbc.update(
            "INSERT INTO products (id, book_id, name, sale_price, discounted_price, cost_price) VALUES (?, ?, '상품', 20000, 18000, 10000)",
            productId, productId);
        jdbc.update("INSERT INTO product_inventory (product_id, on_hand_quantity) VALUES (?, ?)", productId, onHandQuantity);
        for (final long userId : cartUserIds) {
            jdbc.update("INSERT INTO cart_item (cart_id, product_id, quantity) SELECT id, ?, 1 FROM carts WHERE user_id = ?", productId,
                userId);
        }
        return productId;
    }

    private void viewTwice(final long userId, final long productId) {
        insertView(userId, productId);
        insertView(userId, productId);
    }

    private void insertView(final long userId, final long productId) {
        recordProductView.execute(userId, productId);
    }

    private int notificationCount(final long userId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE user_id = ?", Integer.class, userId);
    }

    private void awaitNotificationCount(final long userId, final int expected) throws InterruptedException {
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (notificationCount(userId) != expected && System.nanoTime() < deadline) {
            Thread.sleep(25);
        }
        assertThat(notificationCount(userId)).isEqualTo(expected);
    }
}
