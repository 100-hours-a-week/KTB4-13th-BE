package com.book.core.coupon.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.coupon.domain.Coupon;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
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
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles({"test", "mysql-test"})
@Testcontainers
@Tag("integration")
@Tag("mysql-migration")
class IssueCouponConcurrencyIntegrationTest {
    private static final AtomicLong NEXT_ID = new AtomicLong(8_100_000L);

    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    private IssueCouponUseCase issueCoupon;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 서로_다른_회원이_동시에_요청해도_발급_한도를_넘지_않는다() throws Exception {
        final long couponId = insertCoupon(4);
        final List<Long> userIds = new ArrayList<>();
        for (long userId = 1; userId <= 12; userId++) {
            userIds.add(userId);
        }

        final List<ErrorCode> results = issueConcurrently(couponId, userIds);

        assertThat(results).filteredOn((final ErrorCode result) -> result == null).hasSize(4);
        assertThat(results).filteredOn((final ErrorCode result) -> result == ErrorCode.COUPON_SOLD_OUT).hasSize(8);
        assertCounts(couponId, 4);
    }

    @Test
    void 같은_회원이_동시에_요청하면_한_번만_발급되고_발급량도_한_번만_증가한다() throws Exception {
        final long couponId = insertCoupon(10);

        final List<ErrorCode> results = issueConcurrently(couponId, List.of(42L, 42L, 42L, 42L, 42L));

        assertThat(results).filteredOn((final ErrorCode result) -> result == null).hasSize(1);
        assertThat(results).filteredOn((final ErrorCode result) -> result == ErrorCode.COUPON_ALREADY_ISSUED).hasSize(4);
        assertCounts(couponId, 1);
    }

    @Test
    void 같은_회원에게_서로_다른_쿠폰을_동시에_발급할_수_있다() throws Exception {
        for (int attempt = 0; attempt < 10; attempt++) {
            final long firstCouponId = insertCoupon(1);
            final long secondCouponId = insertCoupon(1);
            final List<Map.Entry<Long, Long>> requests = List.of(Map.entry(firstCouponId, 99L), Map.entry(secondCouponId, 99L));

            assertThat(issueConcurrently(requests)).containsExactly(null, null);
            assertCounts(firstCouponId, 1);
            assertCounts(secondCouponId, 1);
        }
    }

    @Test
    void 기존_트랜잭션의_일반_조회_후_다른_트랜잭션에서_발급해도_중복을_확인한다() throws Exception {
        final long couponId = insertCoupon(2);
        final CountDownLatch snapshotEstablished = new CountDownLatch(1);
        final CountDownLatch issuanceCommitted = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            final Future<ErrorCode> result = executor.submit(() -> {
                try {
                    transactionTemplate.executeWithoutResult((final var status) -> {
                        jdbc.queryForObject("SELECT COUNT(*) FROM owned_coupon WHERE user_id = ?", Integer.class, 123L);
                        snapshotEstablished.countDown();
                        await(issuanceCommitted);
                        issueCoupon.execute(123L, couponId);
                    });
                    return null;
                } catch (final CoreException exception) {
                    return exception.errorCode();
                }
            });
            try {
                assertThat(snapshotEstablished.await(10, TimeUnit.SECONDS)).isTrue();
                issueCoupon.execute(123L, couponId);
            } finally {
                issuanceCommitted.countDown();
            }
            assertThat(result.get(30, TimeUnit.SECONDS)).isEqualTo(ErrorCode.COUPON_ALREADY_ISSUED);
        }
        assertCounts(couponId, 1);
    }

    @Test
    void 기존_트랜잭션에서_조회한_쿠폰도_최신_발급량으로_한도를_검증한다() throws Exception {
        final long couponId = insertCoupon(1);
        final CountDownLatch couponLoaded = new CountDownLatch(1);
        final CountDownLatch issuanceCommitted = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            final Future<ErrorCode> result = executor.submit(() -> {
                try {
                    transactionTemplate.executeWithoutResult((final var status) -> {
                        assertThat(entityManager.find(Coupon.class, couponId)).isNotNull();
                        couponLoaded.countDown();
                        await(issuanceCommitted);
                        issueCoupon.execute(124L, couponId);
                    });
                    return null;
                } catch (final CoreException exception) {
                    return exception.errorCode();
                }
            });
            try {
                assertThat(couponLoaded.await(10, TimeUnit.SECONDS)).isTrue();
                issueCoupon.execute(123L, couponId);
            } finally {
                issuanceCommitted.countDown();
            }
            assertThat(result.get(30, TimeUnit.SECONDS)).isEqualTo(ErrorCode.COUPON_SOLD_OUT);
        }
        assertCounts(couponId, 1);
    }

    @Test
    void 같은_트랜잭션에서_두_회원에게_발급하면_수량이_두_번_증가한다() {
        final long couponId = insertCoupon(2);

        transactionTemplate.executeWithoutResult((final var status) -> {
            issueCoupon.execute(125L, couponId);
            issueCoupon.execute(126L, couponId);
        });

        assertCounts(couponId, 2);
    }

    @Test
    void 회원_ID가_없으면_발급량을_증가시키지_않는다() {
        final long couponId = insertCoupon(2);

        assertThatThrownBy(() -> issueCoupon.execute(null, couponId)).isInstanceOf(RuntimeException.class);

        assertCounts(couponId, 0);
        issueCoupon.execute(1L, couponId);
        assertCounts(couponId, 1);
    }

    private List<ErrorCode> issueConcurrently(final long couponId, final List<Long> userIds) throws Exception {
        return issueConcurrently(userIds.stream().map((final Long userId) -> Map.entry(couponId, userId)).toList());
    }

    private List<ErrorCode> issueConcurrently(final List<Map.Entry<Long, Long>> requests) throws Exception {
        final CountDownLatch ready = new CountDownLatch(requests.size());
        final CountDownLatch start = new CountDownLatch(1);
        final List<Future<ErrorCode>> futures = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(requests.size())) {
            for (final Map.Entry<Long, Long> request : requests) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("동시 발급 시작을 기다리는 시간이 초과됐습니다.");
                    }
                    try {
                        issueCoupon.execute(request.getValue(), request.getKey());
                        return null;
                    } catch (final CoreException exception) {
                        return exception.errorCode();
                    }
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            final List<ErrorCode> results = new ArrayList<>();
            for (final Future<ErrorCode> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            start.countDown();
        }
    }

    private long insertCoupon(final int totalQuantity) {
        final long couponId = NEXT_ID.incrementAndGet();
        jdbc.update("""
            INSERT INTO coupon (id, name, type, discount, max_use_count, min_order_amount, starts_at, expired_at,
                                total_quantity, issued_quantity)
            VALUES (?, '발급 한도 쿠폰', 'FIXED_AMOUNT', 1000, 1, 0, CURRENT_TIMESTAMP(6) - INTERVAL 1 DAY,
                    CURRENT_TIMESTAMP(6) + INTERVAL 1 DAY, ?, 0)
            """, couponId, totalQuantity);
        return couponId;
    }

    private void assertCounts(final long couponId, final int expected) {
        assertThat(jdbc.queryForObject("SELECT issued_quantity FROM coupon WHERE id = ?", Integer.class, couponId)).isEqualTo(expected);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM owned_coupon WHERE coupon_id = ?", Integer.class, couponId))
            .isEqualTo(expected);
    }

    private static void await(final CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("발급 완료를 기다리는 시간이 초과됐습니다.");
            }
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("발급 완료를 기다리는 중 중단됐습니다.", exception);
        }
    }

}
