package com.book.core.coupon.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.core.auth.application.port.TokenIssuer;
import com.book.core.coupon.application.port.CouponStockPort;
import com.book.core.coupon.application.result.CouponReconcileOutcome;
import com.book.core.coupon.application.port.IssuanceDltLogRepositoryPort;
import com.book.core.coupon.domain.IssuanceDltLog;
import com.book.core.coupon.application.usecase.ReconcileCouponStockUseCase;
import com.book.core.coupon.application.usecase.FindOwnedCouponsForCheckoutUseCase;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.domain.Order;
import com.book.core.point.application.port.PointBalanceRepositoryPort;
import com.book.core.point.domain.PointBalance;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.kafka.KafkaContainer;

@SpringBootTest(properties = {"coupon.admin.user-ids=1", "coupon.kafka.worker-enabled=true", "coupon.cache.fresh-ms=60000",
    "coupon.cache.ttl-ms=120000", "coupon.sold-out.ttl-seconds=86400", "coupon.sold-out.fast-path-ttl-ms=60000"})
@AutoConfigureMockMvc
@ActiveProfiles({"test", "mysql-test"})
@Testcontainers
@Tag("integration")
class CouponSoldOutHttpIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Container
    @ServiceConnection
    static final KafkaContainer kafka = new KafkaContainer("apache/kafka:4.1.2");

    @Container
    @ServiceConnection(name = "redis")
    static final GenericContainer<?> redis = new GenericContainer<>("redis:8.0").withExposedPorts(6379);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private IssuanceDltLogRepositoryPort dltLogs;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private TokenIssuer tokens;

    @Autowired
    private CouponStockPort couponStockPort;

    @Autowired
    private ReconcileCouponStockUseCase reconcileCouponStockUseCase;

    @Autowired
    private LoadingCache<String, Boolean> couponSoldOutCache;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListeners;

    @Autowired
    private OrderRepositoryPort orders;

    @Autowired
    private PointBalanceRepositoryPort points;

    @Autowired
    private FindOwnedCouponsForCheckoutUseCase checkoutCoupons;

    @Test
    void 마감_전에_접수한_쿠폰이_만료_뒤_쿠폰함에_반영되면_결제에_사용할_수_없다() throws Exception {
        final long userId = 8_510_017L;
        final long couponId = 8_510_017L;
        final long productId = 8_510_018L;
        final String token = "Bearer " + tokens.issue(userId).accessToken();
        final var worker = kafkaListeners.getListenerContainers().stream()
            .filter(container -> "book-coupon-issuance-redis".equals(container.getGroupId())).findFirst().orElseThrow();

        worker.pause();
        final LocalDateTime expiredAt;
        try {
            await().atMost(Duration.ofSeconds(15)).until(worker::isContainerPaused);
            expiredAt = LocalDateTime.now().plusSeconds(5).withNano(0);
            insertCoupon(couponId, 2, expiredAt.minusDays(1), expiredAt);
            issue(userId, couponId).andExpect(status().isAccepted()).andExpect(jsonPath("$.success").value(true));
            assertThat(LocalDateTime.now()).isBefore(expiredAt);
            assertCounts(couponId, 0);
            mvc.perform(get("/api/v1/owned-coupons").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
            await().atMost(Duration.ofSeconds(10)).until(() -> !LocalDateTime.now().isBefore(expiredAt));
            assertCounts(couponId, 0);
        } finally {
            worker.resume();
        }

        assertCounts(couponId, 1);
        final Long ownedCouponId = jdbc.queryForObject("SELECT id FROM owned_coupon WHERE coupon_id = ?", Long.class, couponId);
        mvc.perform(get("/api/v1/owned-coupons").header(HttpHeaders.AUTHORIZATION, token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(ownedCouponId))
            .andExpect(jsonPath("$.data[0].state").value("ISSUED"))
            .andExpect(jsonPath("$.data[0].expiredAt").value(expiredAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        assertThat(checkoutCoupons.execute(userId, List.of(couponId), expiredAt.minusSeconds(1))).hasSize(1);
        assertThat(checkoutCoupons.execute(userId, List.of(couponId), expiredAt)).isEmpty();

        jdbc.update(
            "INSERT INTO books (id, title, author, publisher, category, published_at) VALUES (?, '책', '저자', '출판사', '소설', '2026-01-01')",
            productId);
        jdbc.update("""
            INSERT INTO products (id, book_id, name, sale_price, discounted_price, cost_price, stock_quantity)
            VALUES (?, ?, '쿠폰 대상 상품', 10000, 10000, 0, 10)
            """, productId, productId);
        jdbc.update("INSERT INTO coupon_target (coupon_id, target_type, target_id) VALUES (?, 'PRODUCT', ?)", couponId, productId);
        points.save(new PointBalance(userId));
        final Order order = Order.create(userId, "coupon-expiry-boundary", null);
        order.addItem(productId, "책", "", "저자", BigDecimal.valueOf(10000), BigDecimal.valueOf(10000), 1);
        orders.save(order);

        mvc.perform(post("/api/v1/payments").header(HttpHeaders.AUTHORIZATION, token).contentType("application/json").content("""
            {"orderKey":"%s","useOwnedCouponId":%d,"usePoint":0}
            """.formatted(order.key(), ownedCouponId))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E5000"));
    }

    @Test
    void 매진_신호를_확인하면_중복_검사와_정책_조회를_건너뛴다() throws Exception {
        final long couponId = 8_510_001L;
        insertActiveCoupon(couponId, 2);

        issue(1L, couponId).andExpect(status().isAccepted());
        assertThat(redisTemplate.hasKey(soldOutKey(couponId))).isFalse();
        issue(2L, couponId).andExpect(status().isAccepted());
        assertThat(redisTemplate.opsForValue().get(stockKey(couponId))).isEqualTo("0");
        assertThat(redisTemplate.opsForValue().get(soldOutKey(couponId))).isEqualTo("1");
        assertThat(redisTemplate.getExpire(soldOutKey(couponId))).isBetween(86_390L, 86_400L);

        // 기존 미매진 캐시가 유효한 동안은 기존 발급 흐름으로 진행한다.
        expectError(1L, couponId, "E4001");
        couponSoldOutCache.invalidate(soldOutKey(couponId));
        redisTemplate.delete(policyKey(couponId));

        expectError(1L, couponId, "E4003");
        assertThat(couponSoldOutCache.getIfPresent(soldOutKey(couponId))).isTrue();
        redisTemplate.delete(soldOutKey(couponId));
        expectError(3L, couponId, "E4003");
        expectError(4L, couponId, "E4003");

        assertThat(redisTemplate.hasKey(policyKey(couponId))).isFalse();
        assertCounts(couponId, 2);
    }

    @Test
    void 매진_키가_없는_기존_쿠폰은_재고_Lua로_확인하고_신호_미조회_결과도_캐시한다() throws Exception {
        final long couponId = 8_510_008L;
        insertActiveCoupon(couponId, 1);
        redisTemplate.opsForValue().set(stockKey(couponId), "0");

        expectError(1L, couponId, "E4003");
        assertThat(couponSoldOutCache.getIfPresent(soldOutKey(couponId))).isFalse();
        redisTemplate.opsForValue().set(soldOutKey(couponId), "1");
        redisTemplate.delete(policyKey(couponId));

        expectError(2L, couponId, "E4003");

        assertThat(redisTemplate.hasKey(policyKey(couponId))).isTrue();
        assertCounts(couponId, 0);
    }

    @Test
    void 매진_신호는_발급_기간과_쿠폰_존재_검사보다_우선한다() throws Exception {
        final LocalDateTime now = LocalDateTime.now();
        final long future = 8_510_002L;
        final long expired = 8_510_003L;
        final long deleted = 8_510_004L;
        final long missing = 8_510_005L;
        insertCoupon(future, 1, now.plusDays(1), now.plusDays(2));
        insertCoupon(expired, 1, now.minusDays(2), now.minusDays(1));
        insertActiveCoupon(deleted, 1);
        jdbc.update("UPDATE coupon SET deleted_at = CURRENT_TIMESTAMP(6) WHERE id = ?", deleted);

        for (final long couponId : new long[] {future, expired, deleted, missing}) {
            redisTemplate.opsForValue().set(soldOutKey(couponId), "1");
            expectError(2L, couponId, "E4003");

            redisTemplate.delete(soldOutKey(couponId));
            couponSoldOutCache.invalidate(soldOutKey(couponId));
            expectError(2L, couponId, "E4000");
        }
    }

    @Test
    void 재고_복구시_매진_신호를_초기화하면_이전_매진_정책이_남아_있어도_발급한다() throws Exception {
        final long couponId = 8_510_006L;
        insertActiveCoupon(couponId, 1);
        issue(1L, couponId).andExpect(status().isAccepted());
        expectError(2L, couponId, "E4003");
        redisTemplate.delete(policyKey(couponId));
        expectError(2L, couponId, "E4003");
        couponSoldOutCache.invalidate(soldOutKey(couponId));
        jdbc.update("UPDATE coupon SET total_quantity = 2 WHERE id = ?", couponId);
        redisTemplate.opsForValue().set(stockKey(couponId), "1");
        assertThat(redisTemplate.hasKey(soldOutKey(couponId))).isTrue();
        expectError(2L, couponId, "E4003");
        assertCounts(couponId, 1);

        couponStockPort.initialize(couponId, 1);
        assertThat(redisTemplate.hasKey(soldOutKey(couponId))).isFalse();
        expectError(1L, couponId, "E4001");
        issue(2L, couponId).andExpect(status().isAccepted());
        assertCounts(couponId, 2);
        assertThat(redisTemplate.opsForValue().get(stockKey(couponId))).isEqualTo("0");
    }

    @Test
    void 미발급_예약과_재고를_임의로_복구한_뒤의_불일치를_구분한다() throws Exception {
        final long couponId = 8_510_007L;
        insertActiveCoupon(couponId, 1);
        jdbc.execute("ALTER TABLE owned_coupon ADD CONSTRAINT test_coupon_rollback CHECK (user_id <> 99 OR coupon_id <> " + couponId + ")");
        try {
            issue(99L, couponId).andExpect(status().isAccepted());
            awaitFailedIssuance(couponId);
            assertThat(couponSoldOutCache.getIfPresent(soldOutKey(couponId))).isFalse();
            final var shortage = reconcileCouponStockUseCase.execute(couponId).orElseThrow();
            assertThat(shortage.totalQuantity()).isEqualTo(1);
            assertThat(shortage.issuedQuantity()).isZero();
            assertThat(shortage.ownedCouponCount()).isZero();
            assertThat(shortage.expectedStock()).isZero();
            assertThat(shortage.pendingReservations()).isOne();
            assertThat(shortage.outcome()).isEqualTo(CouponReconcileOutcome.PENDING);
            assertThat(shortage.redisStock()).isZero();
            assertThat(shortage.stockDifference()).isZero();
            assertThat(shortage.soldOutSignalPresent()).isTrue();
            assertThat(shortage.hasUnexpectedSoldOutSignal()).isFalse();
            assertThat(shortage.hasIssuanceCountMismatch()).isFalse();
            assertThat(shortage.isConsistentlySoldOut()).isFalse();
            assertThat(couponSoldOutCache.getIfPresent(soldOutKey(couponId))).isFalse();
            assertCounts(couponId, 0);
            assertThat(redisTemplate.opsForValue().get(stockKey(couponId))).isEqualTo("0");
            assertThat(redisTemplate.hasKey(soldOutKey(couponId))).isTrue();
            expectError(2L, couponId, "E4003");

            couponStockPort.initialize(couponId, 1);
            assertThat(redisTemplate.hasKey(soldOutKey(couponId))).isFalse();
            issue(2L, couponId).andExpect(status().isAccepted());
            assertCounts(couponId, 1);

            final var soldOut = reconcileCouponStockUseCase.execute(couponId).orElseThrow();
            assertThat(soldOut.totalQuantity()).isEqualTo(1);
            assertThat(soldOut.issuedQuantity()).isEqualTo(1);
            assertThat(soldOut.ownedCouponCount()).isEqualTo(1);
            assertThat(soldOut.expectedStock()).isEqualTo(-1);
            assertThat(soldOut.outcome()).isEqualTo(CouponReconcileOutcome.DRIFT);
            assertThat(soldOut.redisStock()).isZero();
            assertThat(soldOut.stockDifference()).isEqualTo(-1L);
            assertThat(soldOut.soldOutSignalPresent()).isTrue();
            assertThat(soldOut.hasUnexpectedSoldOutSignal()).isFalse();
            assertThat(soldOut.isConsistentlySoldOut()).isFalse();
        } finally {
            jdbc.execute("ALTER TABLE owned_coupon DROP CHECK test_coupon_rollback");
        }
    }

    @Test
    void 재고_키가_없으면_재고_0과_구분하고_삭제된_발급_이력도_집계한다() throws Exception {
        final long couponId = 8_510_009L;
        insertActiveCoupon(couponId, 1);
        issue(1L, couponId).andExpect(status().isAccepted());
        assertCounts(couponId, 1);
        jdbc.update("UPDATE owned_coupon SET deleted_at = CURRENT_TIMESTAMP(6) WHERE coupon_id = ?", couponId);
        redisTemplate.delete(stockKey(couponId));

        final var result = reconcileCouponStockUseCase.execute(couponId).orElseThrow();

        assertThat(result.issuedQuantity()).isEqualTo(1);
        assertThat(result.ownedCouponCount()).isEqualTo(1);
        assertThat(result.redisStock()).isNull();
        assertThat(result.outcome()).isEqualTo(CouponReconcileOutcome.REDIS_STOCK_MISSING);
        assertThat(result.stockDifference()).isNull();
        assertThat(result.soldOutSignalPresent()).isTrue();
        assertThat(result.isConsistentlySoldOut()).isFalse();
        assertThat(redisTemplate.hasKey(stockKey(couponId))).isFalse();
    }

    @Test
    void 실패_원인을_보관하고_선택한_접수만_다시_발급한다() throws Exception {
        final long selectedCoupon = 8_510_010L;
        final long untouchedCoupon = 8_510_011L;
        insertActiveCoupon(selectedCoupon, 2);
        insertActiveCoupon(untouchedCoupon, 2);
        jdbc.execute("ALTER TABLE owned_coupon ADD CONSTRAINT test_coupon_dlt_replay CHECK (coupon_id NOT IN (" + selectedCoupon + ", "
            + untouchedCoupon + "))");

        final long selectedId;
        final long untouchedId;
        try {
            issue(99L, selectedCoupon).andExpect(status().isAccepted());
            issue(99L, untouchedCoupon).andExpect(status().isAccepted());
            selectedId = awaitDltLogId(selectedCoupon);
            untouchedId = awaitDltLogId(untouchedCoupon);
        } finally {
            jdbc.execute("ALTER TABLE owned_coupon DROP CHECK test_coupon_dlt_replay");
        }

        final IssuanceDltLog log = jdbc.queryForObject("""
            SELECT dlt_topic, dlt_partition, dlt_offset, message_key, payload, error_type
            FROM issuance_dlt_log WHERE id = ?
            """, (row, index) -> new IssuanceDltLog(row.getString("dlt_topic"), row.getInt("dlt_partition"), row.getLong("dlt_offset"),
            row.getString("message_key"), row.getString("payload"), row.getString("error_type")), selectedId);
        assertThat(log.errorType()).isNotEqualTo("UNKNOWN");
        assertThat(log.messageKey()).isNotBlank();
        dltLogs.record(log);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM issuance_dlt_log WHERE message_key = ?", Integer.class, log.messageKey()))
            .isOne();

        final var pending = reconcileCouponStockUseCase.execute(selectedCoupon).orElseThrow();
        assertThat(pending.pendingReservations()).isOne();
        assertThat(pending.outcome()).isEqualTo(CouponReconcileOutcome.PENDING);

        final String replay = "/admin-api/v1/coupons/issuance-dlt/replay";
        final String body = "{\"ids\":[" + selectedId + "]}";
        mvc.perform(post(replay).contentType("application/json").content(body).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + tokens.issue(2L).accessToken())).andExpect(status().isForbidden());
        mvc.perform(post(replay).contentType("application/json").content(body).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + tokens.issue(1L).accessToken())).andExpect(status().isOk()).andExpect(jsonPath("$.data").value(1));
        assertCounts(selectedCoupon, 1);
        await().atMost(Duration.ofSeconds(15)).until(() -> Boolean.TRUE
            .equals(jdbc.queryForObject("SELECT replayed_at IS NOT NULL FROM issuance_dlt_log WHERE id = ?", Boolean.class, selectedId)));
        assertCounts(untouchedCoupon, 0);
        assertThat(jdbc.queryForObject("SELECT replayed_at IS NULL FROM issuance_dlt_log WHERE id = ?", Boolean.class, untouchedId))
            .isTrue();
        assertThat(redisTemplate.opsForValue().get(stockKey(selectedCoupon))).isEqualTo("1");

        final var duplicate = kafkaTemplate.send("book.coupon-issuance.redis.v1", log.messageKey(), log.payload()).get(10, TimeUnit.SECONDS)
            .getRecordMetadata();
        final var partition = new TopicPartition(duplicate.topic(), duplicate.partition());
        try (final var admin = Admin.create(Map.of("bootstrap.servers", kafka.getBootstrapServers()))) {
            await().atMost(Duration.ofSeconds(15)).until(() -> {
                final var offsets =
                    admin.listConsumerGroupOffsets("book-coupon-issuance-redis").partitionsToOffsetAndMetadata().get(3, TimeUnit.SECONDS);
                final var committed = offsets.get(partition);
                return committed != null && committed.offset() > duplicate.offset();
            });
        }
        assertCounts(selectedCoupon, 1);
        final var reconciled = reconcileCouponStockUseCase.execute(selectedCoupon).orElseThrow();
        assertThat(reconciled.pendingReservations()).isZero();
        assertThat(reconciled.outcome()).isEqualTo(CouponReconcileOutcome.MATCHED);

        redisTemplate.opsForValue().decrement(stockKey(selectedCoupon));
        final var drift = reconcileCouponStockUseCase.execute(selectedCoupon).orElseThrow();
        assertThat(drift.stockDifference()).isEqualTo(1);
        assertThat(drift.outcome()).isEqualTo(CouponReconcileOutcome.DRIFT);
    }

    private long awaitDltLogId(final long couponId) {
        return await().atMost(Duration.ofSeconds(15)).until(
            () -> jdbc.queryForList("SELECT id FROM issuance_dlt_log WHERE payload LIKE ?", Long.class, "%\"couponId\":" + couponId + "%"),
            ids -> !ids.isEmpty()).getFirst();
    }

    @Test
    void 수량이_일치하면_누락_발급자와_매진_신호를_복구한다() throws Exception {
        final long couponId = 8_510_016L;
        insertActiveCoupon(couponId, 1);
        issue(1L, couponId).andExpect(status().isAccepted());
        assertCounts(couponId, 1);
        final String reservations = "coupon:" + couponId + ":reservations";
        redisTemplate.delete(List.of(reservations, soldOutKey(couponId)));

        final var repaired = reconcileCouponStockUseCase.execute(couponId).orElseThrow();

        assertThat(repaired.outcome()).isEqualTo(CouponReconcileOutcome.MATCHED);
        assertThat(redisTemplate.opsForSet().isMember(reservations, "1")).isTrue();
        assertThat(redisTemplate.hasKey(soldOutKey(couponId))).isTrue();
        assertThat(redisTemplate.opsForValue().get(stockKey(couponId))).isEqualTo("0");
    }

    private void awaitFailedIssuance(final long couponId) {
        final Map<String, Object> settings =
            Map.of("bootstrap.servers", kafka.getBootstrapServers(), "group.id", "coupon-dlt-test", "auto.offset.reset", "earliest");
        try (final var consumer = new KafkaConsumer<>(settings, new StringDeserializer(), new StringDeserializer())) {
            consumer.subscribe(List.of("book.coupon-issuance.redis.v1.DLT"));
            await().atMost(Duration.ofSeconds(15)).until(() -> {
                for (final var record : consumer.poll(Duration.ofMillis(200))) {
                    if (record.value().contains("\"couponId\":" + couponId)) {
                        return true;
                    }
                }
                return false;
            });
        }
    }

    private void insertActiveCoupon(final long couponId, final int quantity) {
        final LocalDateTime now = LocalDateTime.now();
        insertCoupon(couponId, quantity, now.minusDays(1), now.plusDays(1));
    }

    private void insertCoupon(final long couponId, final int quantity, final LocalDateTime startsAt, final LocalDateTime expiredAt) {
        jdbc.update("""
            INSERT INTO coupon (id, name, type, discount, max_use_count, min_order_amount,
                                starts_at, expired_at, total_quantity, issued_quantity)
            VALUES (?, '발급 테스트 쿠폰', 'FIXED_AMOUNT', 1000, 1, 0, ?, ?, ?, 0)
            """, couponId, startsAt, expiredAt, quantity);
        redisTemplate.opsForValue().set(stockKey(couponId), Integer.toString(quantity));
    }

    private ResultActions issue(final long userId, final long couponId) throws Exception {
        return mvc.perform(post("/api/v1/coupons/{couponId}/issue", couponId).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + tokens.issue(userId).accessToken()));
    }

    private void expectError(final long userId, final long couponId, final String code) throws Exception {
        issue(userId, couponId).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(code));
    }

    private void assertCounts(final long couponId, final int expected) {
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            assertThat(jdbc.queryForObject("SELECT issued_quantity FROM coupon WHERE id = ?", Integer.class, couponId)).isEqualTo(expected);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM owned_coupon WHERE coupon_id = ?", Integer.class, couponId))
                .isEqualTo(expected);
        });
    }

    private String stockKey(final long couponId) {
        return "coupon:" + couponId + ":stock";
    }

    private String soldOutKey(final long couponId) {
        return "coupon:" + couponId + ":sold_out";
    }

    private String policyKey(final long couponId) {
        return "coupon:" + couponId + ":issue-policy";
    }
}
