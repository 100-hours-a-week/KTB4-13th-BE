package com.book.core.product.infrastructure.persistence.repository;

import static com.book.core.book.domain.QBook.book;
import static com.book.core.category.domain.QCategory.category;
import static com.book.core.product.domain.QProduct.product;
import static com.book.core.product.domain.QProductCategory.productCategory;

import com.book.core.product.application.command.ProductListCursor;
import com.book.core.product.application.command.ProductListSort;
import com.book.core.product.application.result.ProductListItem;
import com.book.core.product.domain.QProduct;
import com.book.core.product.infrastructure.persistence.entity.QProductPopularitySnapshot;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class ProductQueryRepository {
    private static final String INSERT_POPULARITY_SNAPSHOTS_SQL = """
        INSERT INTO product_popularity_snapshots (
            product_id,
            sales_quantity,
            review_count,
            review_rate,
            refreshed_at
        )
        SELECT
            product.id,
            COALESCE(sales.sales_quantity, 0),
            COALESCE(reviews.review_count, 0),
            COALESCE(reviews.review_rate, 0),
            CURRENT_TIMESTAMP(6)
        FROM products product
        LEFT JOIN (
            SELECT order_item.product_id, SUM(order_item.quantity) AS sales_quantity
            FROM order_item
            JOIN orders ON orders.id = order_item.order_id
            WHERE order_item.status = 'PAID'
              AND order_item.deleted_at IS NULL
              AND orders.deleted_at IS NULL
            GROUP BY order_item.product_id
        ) sales ON sales.product_id = product.id
        LEFT JOIN (
            SELECT order_item.product_id, COUNT(review.id) AS review_count, AVG(review.rating) AS review_rate
            FROM reviews review
            JOIN order_item ON order_item.id = review.order_item_id
            JOIN orders ON orders.id = order_item.order_id
            WHERE review.deleted_at IS NULL
              AND order_item.deleted_at IS NULL
              AND orders.deleted_at IS NULL
            GROUP BY order_item.product_id
        ) reviews ON reviews.product_id = product.id
        """;

    private final JPAQueryFactory queryFactory;
    private final JdbcTemplate jdbcTemplate;

    void refreshPopularitySnapshots() {
        jdbcTemplate.update("DELETE FROM product_popularity_snapshots");
        jdbcTemplate.update(INSERT_POPULARITY_SNAPSHOTS_SQL);
    }

    List<ProductListItem> findActiveProducts(final Long categoryId, final ProductListSort sort, final ProductListCursor cursor,
        final int limit) {
        if (sort == ProductListSort.POPULARITY) {
            return findByPopularity(categoryId, cursor, limit);
        }
        return findByCreatedAt(categoryId, cursor, limit);
    }

    private List<ProductListItem> findByCreatedAt(final Long categoryId, final ProductListCursor cursor, final int limit) {
        return queryFactory.selectFrom(product).join(product.book, book).fetchJoin()
            .where(product.deletedAt.isNull(), book.deletedAt.isNull(), createdAtCursorPredicate(cursor), categoryPredicate(categoryId))
            .orderBy(product.createdAt.desc(), product.id.desc()).limit(limit).fetch().stream()
            .map(item -> new ProductListItem(item, 0L, 0L, BigDecimal.ZERO)).toList();
    }

    private List<ProductListItem> findByPopularity(final Long categoryId, final ProductListCursor cursor, final int limit) {
        final var popularity = popularityExpressions();
        return queryFactory.select(product, popularity.salesQuantity(), popularity.reviewCount(), popularity.reviewRate()).from(product)
            .leftJoin(popularity.snapshot()).on(popularity.snapshot().productId.eq(product.id)).join(product.book, book).fetchJoin()
            .where(product.deletedAt.isNull(), book.deletedAt.isNull(), popularityCursorPredicate(cursor, popularity),
                categoryPredicate(categoryId))
            .orderBy(popularity.salesQuantity().desc(), popularity.reviewCount().desc(), popularity.reviewRate().desc(), product.id.desc())
            .limit(limit).fetch().stream().map(tuple -> toProductListItem(tuple, popularity)).toList();
    }

    private ProductListItem toProductListItem(final Tuple tuple, final PopularityExpressions popularity) {
        return new ProductListItem(tuple.get(product), tuple.get(popularity.salesQuantity()), tuple.get(popularity.reviewCount()),
            tuple.get(popularity.reviewRate()));
    }

    private BooleanExpression createdAtCursorPredicate(final ProductListCursor cursor) {
        if (cursor == null) {
            return null;
        }
        final var cursorProduct = new QProduct("cursorProduct");
        final var cursorCreatedAt =
            JPAExpressions.select(cursorProduct.createdAt).from(cursorProduct).where(cursorProduct.id.eq(cursor.productId()));
        return product.createdAt.lt(cursorCreatedAt).or(product.createdAt.eq(cursorCreatedAt).and(product.id.lt(cursor.productId())));
    }

    private BooleanExpression popularityCursorPredicate(final ProductListCursor cursor, final PopularityExpressions popularity) {
        if (cursor == null) {
            return null;
        }
        return popularity.salesQuantity().lt(cursor.salesQuantity())
            .or(popularity.salesQuantity().eq(cursor.salesQuantity()).and(popularity.reviewCount().lt(cursor.reviewCount())))
            .or(popularity.salesQuantity().eq(cursor.salesQuantity()).and(popularity.reviewCount().eq(cursor.reviewCount()))
                .and(popularity.reviewRate().lt(cursor.reviewRate())))
            .or(popularity.salesQuantity().eq(cursor.salesQuantity()).and(popularity.reviewCount().eq(cursor.reviewCount()))
                .and(popularity.reviewRate().eq(cursor.reviewRate())).and(product.id.lt(cursor.productId())));
    }

    private PopularityExpressions popularityExpressions() {
        final var snapshot = new QProductPopularitySnapshot("productPopularitySnapshot");
        return new PopularityExpressions(snapshot, snapshot.salesQuantity.coalesce(0L), snapshot.reviewCount.coalesce(0L),
            snapshot.reviewRate.coalesce(BigDecimal.ZERO));
    }

    private BooleanExpression categoryPredicate(final Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return JPAExpressions.selectOne().from(productCategory).join(productCategory.category, category)
            .where(productCategory.product.eq(product), productCategory.deletedAt.isNull(), category.id.eq(categoryId),
                category.deletedAt.isNull())
            .exists();
    }

    private record PopularityExpressions(QProductPopularitySnapshot snapshot, NumberExpression<Long> salesQuantity,
        NumberExpression<Long> reviewCount, NumberExpression<BigDecimal> reviewRate) {}
}
