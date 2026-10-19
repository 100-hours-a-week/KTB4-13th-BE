package com.book.core.coupon.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.coupon.application.port.CouponTargetRepositoryPort;
import com.book.core.coupon.application.usecase.GetCouponTargetsUseCase;
import com.book.core.coupon.domain.CouponTarget;
import com.book.core.coupon.domain.enums.CouponTargetType;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles({"test", "mysql"})
@Testcontainers
@Tag("integration")
class CouponTargetRepositoryIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    CouponTargetJpaRepository couponTargetJpaRepository;
    @Autowired
    CouponTargetRepositoryPort couponTargetRepository;
    @Autowired
    GetCouponTargetsUseCase getCouponTargets;
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void 상품과_카테고리_대상을_합치고_삭제된_연결을_제외한다() {
        jdbc.update("INSERT INTO books (id, title, author, publisher, category, published_at) VALUES (?, ?, ?, ?, ?, ?)", 88001L, "도서",
            "저자", "출판사", "소설", "2026-01-01");
        jdbc.update("""
            INSERT INTO products (id, book_id, name, sale_price, discounted_price, cost_price, stock_quantity)
            VALUES (88002, 88001, '상품', 1000, 900, 500, 10)
            """);
        jdbc.update("INSERT INTO book_category (id, name, path) VALUES (88003, '소설', '소설')");
        jdbc.update("INSERT INTO product_category (category_id, product_id) VALUES (88003, 88002)");
        final long productCouponId = 88004L;
        final long categoryCouponId = 88005L;
        couponTargetJpaRepository.saveAndFlush(new CouponTarget(null, productCouponId, CouponTargetType.PRODUCT, 88002L));
        couponTargetJpaRepository.saveAndFlush(new CouponTarget(null, productCouponId, CouponTargetType.PRODUCT_CATEGORY, 88003L));
        couponTargetJpaRepository.saveAndFlush(new CouponTarget(null, categoryCouponId, CouponTargetType.PRODUCT_CATEGORY, 88003L));
        final var deletedTarget = couponTargetJpaRepository.saveAndFlush(new CouponTarget(null, 999L, CouponTargetType.PRODUCT, 88002L));
        deletedTarget.delete();
        couponTargetJpaRepository.saveAndFlush(deletedTarget);
        assertThat(getCouponTargets.execute(List.of(88002L))).containsExactlyInAnyOrder(productCouponId, categoryCouponId);
        jdbc.update("UPDATE product_category SET deleted_at = NOW(6) WHERE product_id = 88002");
        assertThat(couponTargetRepository.findCategoryIdsByProductIds(List.of(88002L))).isEmpty();
        assertThat(getCouponTargets.execute(List.of(88002L))).containsExactly(productCouponId);
    }
}
