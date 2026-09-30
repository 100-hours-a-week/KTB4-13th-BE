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
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
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

    List<ProductListItem> findActiveProducts(final Long categoryId, final LocalDate publishedFrom, final LocalDate publishedTo,
        final ProductListSort sort, final ProductListCursor cursor, final int limit) {
        final BooleanExpression publishedAtPredicate = publishedAtPredicate(publishedFrom, publishedTo);
        if (sort == ProductListSort.POPULARITY) {
            return findByPopularity(categoryId, publishedAtPredicate, cursor, limit);
        }
        return findByCreatedAt(categoryId, publishedAtPredicate, cursor, limit);
    }

    private List<ProductListItem> findByCreatedAt(final Long categoryId, final BooleanExpression publishedAtPredicate,
        final ProductListCursor cursor, final int limit) {
        return queryFactory.selectFrom(product).join(product.book, book).fetchJoin()
            .where(product.deletedAt.isNull(), book.deletedAt.isNull(), createdAtCursorPredicate(cursor),
                categoryPredicate(categoryId, product.id), publishedAtPredicate)
            .orderBy(product.createdAt.desc(), product.id.desc()).limit(limit).fetch().stream()
            .map(item -> new ProductListItem(item, 0L, 0L, BigDecimal.ZERO)).toList();
    }

    private List<ProductListItem> findByPopularity(final Long categoryId, final BooleanExpression publishedAtPredicate,
        final ProductListCursor cursor, final int limit) {
        final var snapshot = new QProductPopularitySnapshot("productPopularitySnapshot");
        // ponytail: scalar-subquery plan; recheck EXPLAIN if MySQL optimizer settings change.
        final var activeProductId = JPAExpressions.select(product.id).from(product).join(product.book, book)
            .where(product.id.eq(snapshot.productId), product.deletedAt.isNull(), book.deletedAt.isNull(), publishedAtPredicate);
        final var ranked =
            queryFactory.select(snapshot.productId, snapshot.salesQuantity, snapshot.reviewCount, snapshot.reviewRate).from(snapshot)
                .where(snapshot.productId.eq(activeProductId), popularityCursorPredicate(cursor, snapshot),
                    categoryPredicate(categoryId, snapshot.productId))
                .orderBy(snapshot.salesQuantity.desc(), snapshot.productId.desc()).limit(limit).fetch();
        if (ranked.isEmpty()) {
            return List.of();
        }
        final var ids = ranked.stream().map((final var tuple) -> tuple.get(snapshot.productId)).toList();
        final var products = queryFactory.selectFrom(product).join(product.book, book).fetchJoin().where(product.id.in(ids)).fetch()
            .stream().collect(Collectors.toMap((final var item) -> item.id(), Function.identity()));
        return ranked.stream().map((final var tuple) -> new ProductListItem(products.get(tuple.get(snapshot.productId)),
            tuple.get(snapshot.salesQuantity), tuple.get(snapshot.reviewCount), tuple.get(snapshot.reviewRate))).toList();
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

    private BooleanExpression popularityCursorPredicate(final ProductListCursor cursor, final QProductPopularitySnapshot snapshot) {
        if (cursor == null) {
            return null;
        }
        return snapshot.salesQuantity.lt(cursor.salesQuantity())
            .or(snapshot.salesQuantity.eq(cursor.salesQuantity()).and(snapshot.productId.lt(cursor.productId())));
    }

    private BooleanExpression categoryPredicate(final Long categoryId, final NumberExpression<Long> productId) {
        if (categoryId == null) {
            return null;
        }
        return JPAExpressions.selectOne().from(productCategory).join(productCategory.category, category)
            .where(productCategory.product.id.eq(productId), productCategory.deletedAt.isNull(), category.id.eq(categoryId),
                category.deletedAt.isNull())
            .exists();
    }

    private BooleanExpression publishedAtPredicate(final LocalDate publishedFrom, final LocalDate publishedTo) {
        final BooleanExpression fromPredicate = publishedFrom == null ? null : book.publishedAt.goe(publishedFrom);
        final BooleanExpression toPredicate = publishedTo == null ? null : book.publishedAt.loe(publishedTo);
        return Expressions.allOf(fromPredicate, toPredicate);
    }

}
