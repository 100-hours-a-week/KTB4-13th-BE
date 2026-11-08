package com.book.core.coupon.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.core.auth.application.port.TokenIssuer;
import com.book.core.coupon.application.port.CouponStockPort;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest(properties = {"coupon.cache.fresh-ms=60000", "coupon.cache.ttl-ms=120000", "coupon.sold-out.ttl-seconds=86400",
    "coupon.sold-out.fast-path-ttl-ms=60000"})
@AutoConfigureMockMvc
@ActiveProfiles({"test", "mysql-test"})
@Testcontainers
@Tag("integration")
class CouponSoldOutHttpIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Container
    @ServiceConnection(name = "redis")
    static final GenericContainer<?> redis = new GenericContainer<>("redis:8.0").withExposedPorts(6379);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private TokenIssuer tokens;

    @Autowired
    private CouponStockPort couponStockPort;

    @Autowired
    private LoadingCache<String, Boolean> couponSoldOutCache;

    @BeforeEach
    void resetSharedUserRateLimits() {
        redisTemplate.delete(List.of("coupon:issue-rate-limit:user:1", "coupon:issue-rate-limit:user:2"));
    }

    @Test
    void 같은_사용자가_없는_쿠폰_ID를_바꾸어도_발급_요청_한도를_공유한다() throws Exception {
        final long userId = 9_910_001L;
        final long firstCouponId = 9_910_010L;
        for (long couponId = firstCouponId; couponId < firstCouponId + 10; couponId++) {
            expectError(userId, couponId, "E4000");
        }

        final long blockedCouponId = firstCouponId + 10;
        issue(userId, blockedCouponId).andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.code").value("E429"))
            .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
        assertThat(redisTemplate.hasKey(policyKey(blockedCouponId))).isFalse();

        redisTemplate.opsForValue().set(soldOutKey(blockedCouponId), "1");
        couponSoldOutCache.invalidate(soldOutKey(blockedCouponId));
        expectError(userId, blockedCouponId, "E4003");
    }

    @Test
    void 매진_신호를_확인하면_중복_검사와_정책_조회를_건너뛴다() throws Exception {
        final long couponId = 8_510_001L;
        insertActiveCoupon(couponId, 2);

        issue(1L, couponId).andExpect(status().isOk());
        assertThat(redisTemplate.hasKey(soldOutKey(couponId))).isFalse();
        issue(2L, couponId).andExpect(status().isOk());
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
        issue(1L, couponId).andExpect(status().isOk());
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
        issue(2L, couponId).andExpect(status().isOk());
        assertCounts(couponId, 2);
        assertThat(redisTemplate.opsForValue().get(stockKey(couponId))).isEqualTo("0");
    }

    @Test
    void 마지막_차감_뒤_DB가_롤백되면_재고_복구_후_다시_발급할_수_있다() throws Exception {
        final long couponId = 8_510_007L;
        insertActiveCoupon(couponId, 1);
        jdbc.execute("ALTER TABLE owned_coupon ADD CONSTRAINT test_coupon_rollback CHECK (user_id <> 99)");
        try {
            issue(99L, couponId).andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("E500"));
            assertCounts(couponId, 0);
            assertThat(redisTemplate.opsForValue().get(stockKey(couponId))).isEqualTo("0");
            assertThat(redisTemplate.hasKey(soldOutKey(couponId))).isTrue();
            expectError(2L, couponId, "E4003");

            couponStockPort.initialize(couponId, 1);
            assertThat(redisTemplate.hasKey(soldOutKey(couponId))).isFalse();
            issue(2L, couponId).andExpect(status().isOk());
            assertCounts(couponId, 1);
        } finally {
            jdbc.execute("ALTER TABLE owned_coupon DROP CHECK test_coupon_rollback");
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
        assertThat(jdbc.queryForObject("SELECT issued_quantity FROM coupon WHERE id = ?", Integer.class, couponId)).isEqualTo(expected);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM owned_coupon WHERE coupon_id = ?", Integer.class, couponId))
            .isEqualTo(expected);
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
