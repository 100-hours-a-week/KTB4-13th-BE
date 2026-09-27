package com.book.core.product.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.core.product.application.command.ProductListCursor;
import com.book.core.product.application.command.ProductListSort;
import com.book.core.product.application.port.ProductRepositoryPort;
import com.book.core.product.application.service.ProductService;
import java.math.BigDecimal;
import java.sql.Timestamp;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
class ProductRepositoryIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    ProductRepositoryPort productRepository;

    @Autowired
    ProductService productService;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void 활성_상품과_활성_도서가_연결된_상세만_조회한다() {
        insertBook(1001L, "활성 도서", null);
        insertBook(1002L, "삭제 도서", Timestamp.valueOf("2026-01-01 00:00:00"));
        insertProduct(2001L, 1001L, "활성 상품", null);
        insertProduct(2002L, 1001L, "삭제 상품", Timestamp.valueOf("2026-01-01 00:00:00"));
        insertProduct(2003L, 1002L, "도서가 삭제된 상품", null);

        final var found = productRepository.findActiveById(2001L).orElseThrow();

        assertThat(found.name()).isEqualTo("활성 상품");
        assertThat(found.book().title()).isEqualTo("활성 도서");
        assertThat(productRepository.findActiveById(2002L)).isEmpty();
        assertThat(productRepository.findActiveById(2003L)).isEmpty();
    }

    @Test
    void 카테고리와_활성_조건을_적용하고_생성일과_ID_기준_커서로_상품을_조회한다() {
        insertCategory(3001L, "소설", null);
        insertCategory(3002L, "삭제 카테고리", Timestamp.valueOf("2026-01-01 00:00:00"));
        insertBook(1003L, "목록 도서 1", null);
        insertBook(1004L, "목록 도서 2", null);
        insertBook(1005L, "목록 도서 3", null);
        insertBook(1006L, "삭제 도서", Timestamp.valueOf("2026-01-01 00:00:00"));
        insertBook(1007L, "비활성 연결 도서", null);
        insertProduct(2101L, 1003L, "상품 1", null, Timestamp.valueOf("2026-01-03 00:00:00"));
        insertProduct(2102L, 1004L, "상품 2", null, Timestamp.valueOf("2026-01-01 00:00:00"));
        insertProduct(2103L, 1005L, "상품 3", null, Timestamp.valueOf("2026-01-02 00:00:00"));
        insertProduct(2104L, 1006L, "삭제 도서 상품", null, Timestamp.valueOf("2026-01-01 00:00:00"));
        insertProduct(2105L, 1007L, "비활성 연결 상품", null, Timestamp.valueOf("2026-01-01 00:00:00"));
        insertProductCategory(3101L, 3001L, 2101L, null);
        insertProductCategory(3102L, 3001L, 2102L, null);
        insertProductCategory(3103L, 3001L, 2103L, null);
        insertProductCategory(3104L, 3001L, 2104L, null);
        insertProductCategory(3105L, 3002L, 2101L, null);
        insertProductCategory(3106L, 3001L, 2105L, Timestamp.valueOf("2026-01-01 00:00:00"));

        final var firstPage = productRepository.findActiveProducts(3001L, ProductListSort.CREATED_AT, null, 1);
        final var secondPage =
            productRepository.findActiveProducts(3001L, ProductListSort.CREATED_AT, new ProductListCursor(2101L, null, null, null), 2);

        assertThat(firstPage).extracting(item -> item.product().id()).containsExactly(2101L);
        assertThat(secondPage).extracting(item -> item.product().id()).containsExactly(2103L, 2102L);
    }

    @Test
    void 인기순은_결제수량_리뷰수_평균평점_ID_순으로_정렬하고_커서와_기존_필터를_적용한다() {
        insertCategory(3003L, "활성 카테고리", null);
        insertCategory(3004L, "다른 카테고리", null);
        insertBook(1010L, "인기순 도서", null);
        insertBook(1011L, "삭제 도서", Timestamp.valueOf("2026-01-01 00:00:00"));
        insertProduct(2201L, 1010L, "인기 상품 1", null);
        insertProduct(2202L, 1010L, "인기 상품 2", null);
        insertProduct(2203L, 1010L, "판매량 우선 상품", null);
        insertProduct(2204L, 1010L, "평점 우선 상품", null);
        insertProduct(2205L, 1010L, "리뷰 수가 적은 상품", null);
        insertProduct(2206L, 1010L, "인기 데이터 없음", null);
        insertProduct(2209L, 1010L, "인기 데이터 없음 ID 동률", null);
        insertProduct(2290L, 1010L, "삭제 상품", Timestamp.valueOf("2026-01-01 00:00:00"));
        insertProduct(2291L, 1011L, "삭제 도서 상품", null);
        for (final long productId : new long[] {2201L, 2202L, 2203L, 2204L, 2205L, 2206L, 2209L, 2290L, 2291L}) {
            insertProductCategory(3200L + productId, 3003L, productId, null);
        }
        insertProductCategory(3301L, 3004L, 2206L, null);

        insertOrderAddress(4100L);
        insertPaidItem(4201L, 2201L, 5, null, null);
        insertReview(4301L, 4201L, "8.0", null);
        insertReview(4302L, 4201L, "8.0", null);
        insertPaidItem(4202L, 2202L, 5, null, null);
        insertReview(4303L, 4202L, "9.0", null);
        insertReview(4304L, 4202L, "7.0", null);
        insertPaidItem(4203L, 2203L, 6, null, null);
        insertPaidItem(4204L, 2204L, 5, null, null);
        insertReview(4305L, 4204L, "9.0", null);
        insertReview(4306L, 4204L, "9.0", null);
        insertPaidItem(4205L, 2205L, 5, null, null);
        insertReview(4307L, 4205L, "10.0", null);
        insertPaidItem(4206L, 2206L, 100, "CREATED", null);
        insertPaidItem(4207L, 2206L, 100, "CANCELED", null);
        insertPaidItem(4208L, 2206L, 100, "PAID", Timestamp.valueOf("2026-01-01 00:00:00"));
        insertPaidItem(4209L, 2206L, 100, "PAID", null, Timestamp.valueOf("2026-01-01 00:00:00"));
        insertReview(4308L, 4208L, "10.0", null);
        insertReview(4309L, 4205L, "0.0", Timestamp.valueOf("2026-01-01 00:00:00"));
        insertPaidItem(4210L, 2290L, 1000, null, null);
        insertPaidItem(4211L, 2291L, 1000, null, null);

        productService.refreshProductPopularitySnapshot();

        final var firstPage = productRepository.findActiveProducts(3003L, ProductListSort.POPULARITY, null, 3);
        final var last = firstPage.getLast();
        final var cursor = new ProductListCursor(last.product().id(), last.salesQuantity(), last.reviewCount(), last.reviewRate());
        final var secondPage = productRepository.findActiveProducts(3003L, ProductListSort.POPULARITY, cursor, 10);

        assertThat(firstPage).extracting(item -> item.product().id()).containsExactly(2203L, 2204L, 2202L);
        assertThat(secondPage).extracting(item -> item.product().id()).containsExactly(2201L, 2205L, 2209L, 2206L);
        final var oneActiveReview = secondPage.stream().filter(item -> item.product().id() == 2205L).findFirst().orElseThrow();
        assertThat(oneActiveReview.reviewCount()).isEqualTo(1L);
        assertThat(oneActiveReview.reviewRate()).isEqualByComparingTo("10.0");
        assertThat(secondPage.getLast().salesQuantity()).isZero();
        assertThat(secondPage.getLast().reviewCount()).isZero();
        assertThat(secondPage.getLast().reviewRate()).isEqualByComparingTo(BigDecimal.ZERO);

        jdbc.update("UPDATE order_item SET quantity = 9 WHERE id = 4203");
        productService.refreshProductPopularitySnapshot();
        productService.refreshProductPopularitySnapshot();

        final var refreshed = productRepository.findActiveProducts(3003L, ProductListSort.POPULARITY, null, 10).stream()
            .filter(item -> item.product().id() == 2203L).findFirst().orElseThrow();
        assertThat(refreshed.salesQuantity()).isEqualTo(9L);
    }

    @Test
    void 갱신_중_오류가_발생하면_마지막_성공_스냅샷을_유지한다() {
        insertBook(1020L, "갱신 실패 검증 도서", null);
        insertProduct(5201L, 1020L, "갱신 실패 검증 상품", null);
        insertOrderAddress(5200L);
        insertPaidItem(5202L, 5201L, 2, null, null, null, 5200L);
        productService.refreshProductPopularitySnapshot();
        jdbc.update("UPDATE order_item SET quantity = 9 WHERE id = 5202");
        jdbc.execute("""
            ALTER TABLE product_popularity_snapshots
            ADD CONSTRAINT chk_fail_popularity_snapshot_refresh
            CHECK (product_id <> 5201 OR sales_quantity <> 9)
            """);

        try {
            assertThatThrownBy(() -> productService.refreshProductPopularitySnapshot()).isInstanceOf(DataAccessException.class);
        } finally {
            jdbc.execute("ALTER TABLE product_popularity_snapshots DROP CHECK chk_fail_popularity_snapshot_refresh");
        }

        final Long salesQuantity =
            jdbc.queryForObject("SELECT sales_quantity FROM product_popularity_snapshots WHERE product_id = ?", Long.class, 5201L);
        assertThat(salesQuantity).isEqualTo(2L);
    }

    private void insertPaidItem(final long id, final long productId, final int quantity, final String status, final Timestamp deletedAt) {
        insertPaidItem(id, productId, quantity, status, deletedAt, null);
    }

    private void insertPaidItem(final long id, final long productId, final int quantity, final String status, final Timestamp deletedItemAt,
        final Timestamp deletedOrderAt) {
        insertPaidItem(id, productId, quantity, status, deletedItemAt, deletedOrderAt, 4100L);
    }

    private void insertPaidItem(final long id, final long productId, final int quantity, final String status, final Timestamp deletedItemAt,
        final Timestamp deletedOrderAt, final long addressId) {
        final String itemStatus;
        if (status == null) {
            itemStatus = "PAID";
        } else {
            itemStatus = status;
        }
        jdbc.update("""
                INSERT INTO orders (id, user_id, `key`, total_price, status, address_id, deleted_at)
                VALUES (?, ?, ?, ?, 'PAID', ?, ?)
                """, id, id, "popularity-order-" + id, BigDecimal.valueOf(quantity * 100L), addressId, deletedOrderAt);
        jdbc.update("""
                INSERT INTO order_item (
                    id, order_id, product_id, unit_price, total_price, quantity, status, deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, id, id, productId, BigDecimal.valueOf(100L), BigDecimal.valueOf(quantity * 100L), quantity, itemStatus, deletedItemAt);
    }

    private void insertReview(final long id, final long orderItemId, final String rating, final Timestamp deletedAt) {
        jdbc.update("""
                INSERT INTO reviews (id, user_id, order_item_id, rating, content, deleted_at)
                VALUES (?, ?, ?, ?, 'review', ?)
                """, id, id, orderItemId, new BigDecimal(rating), deletedAt);
    }

    private void insertOrderAddress(final long id) {
        jdbc.update("""
                INSERT INTO order_addresses (id, postal_code, address, detail_address)
                VALUES (?, '00000', 'address', NULL)
                """, id);
    }

    private void insertBook(final long id, final String title, final Timestamp deletedAt) {
        jdbc.update("""
                INSERT INTO books (
                    id, title, author, publisher, category, published_at, deleted_at, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, id, title, "작가", "출판사", "소설", "2026-01-01", deletedAt);
    }

    private void insertProduct(final long id, final long bookId, final String name, final Timestamp deletedAt) {
        insertProduct(id, bookId, name, deletedAt, Timestamp.valueOf("2026-01-01 00:00:00"));
    }

    private void insertProduct(final long id, final long bookId, final String name, final Timestamp deletedAt, final Timestamp createdAt) {
        jdbc.update("""
                INSERT INTO products (
                    id, book_id, name, sale_price, discounted_price, cost_price, stock_quantity,
                    deleted_at, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, bookId, name, 20000.00, 18000.00, 12000.00, 10, deletedAt, createdAt, createdAt);
    }

    private void insertCategory(final long id, final String name, final Timestamp deletedAt) {
        jdbc.update("""
                INSERT INTO book_category (id, name, path, deleted_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, id, name, "도서/" + name, deletedAt);
    }

    private void insertProductCategory(final long id, final long categoryId, final long productId, final Timestamp deletedAt) {
        jdbc.update("""
                INSERT INTO product_category (id, category_id, product_id, deleted_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, id, categoryId, productId, deletedAt);
    }
}
