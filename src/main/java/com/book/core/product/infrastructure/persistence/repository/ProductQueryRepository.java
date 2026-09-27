package com.book.core.product.infrastructure.persistence.repository;

import static com.book.core.book.domain.QBook.book;
import static com.book.core.category.domain.QCategory.category;
import static com.book.core.product.domain.QProduct.product;
import static com.book.core.product.domain.QProductCategory.productCategory;

import com.book.core.order.domain.OrderItemStatus;
import com.book.core.order.domain.QOrder;
import com.book.core.order.domain.QOrderItem;
import com.book.core.product.application.command.ProductListCursor;
import com.book.core.product.application.command.ProductListSort;
import com.book.core.product.application.result.ProductListItem;
import com.book.core.product.domain.QProduct;
import com.book.core.review.domain.QReview;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class ProductQueryRepository {
    private final JPAQueryFactory queryFactory;

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
            .join(product.book, book).fetchJoin()
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
        final var paidOrderItem = new QOrderItem("paidOrderItem");
        final var paidOrder = new QOrder("paidOrder");
        final NumberExpression<Long> salesQuantity = Expressions.numberTemplate(Long.class, "({0})",
            JPAExpressions.select(Expressions.numberTemplate(Long.class, "coalesce(sum({0}), 0)", paidOrderItem.quantity))
                .from(paidOrderItem).join(paidOrderItem.order, paidOrder).where(paidOrderItem.productId.eq(product.id),
                    paidOrderItem.status.eq(OrderItemStatus.PAID), paidOrderItem.deletedAt.isNull(), paidOrder.deletedAt.isNull()));

        final var review = new QReview("activeReview");
        final var reviewOrderItem = new QOrderItem("reviewOrderItem");
        final var reviewOrder = new QOrder("reviewOrder");
        final BooleanExpression activeReview =
            review.deletedAt.isNull().and(reviewOrderItem.deletedAt.isNull()).and(reviewOrder.deletedAt.isNull());
        final NumberExpression<Long> reviewCount = Expressions.numberTemplate(Long.class, "({0})",
            JPAExpressions.select(Expressions.numberTemplate(Long.class, "count({0})", review.id)).from(review).join(reviewOrderItem)
                .on(review.orderItemId.eq(reviewOrderItem.id)).join(reviewOrderItem.order, reviewOrder)
                .where(reviewOrderItem.productId.eq(product.id), activeReview));
        final NumberExpression<BigDecimal> reviewRate = Expressions.numberTemplate(BigDecimal.class, "({0})",
            JPAExpressions.select(Expressions.numberTemplate(BigDecimal.class, "coalesce(avg({0}), 0)", review.rating)).from(review)
                .join(reviewOrderItem).on(review.orderItemId.eq(reviewOrderItem.id)).join(reviewOrderItem.order, reviewOrder)
                .where(reviewOrderItem.productId.eq(product.id), activeReview));
        return new PopularityExpressions(salesQuantity, reviewCount, reviewRate);
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

    private record PopularityExpressions(NumberExpression<Long> salesQuantity, NumberExpression<Long> reviewCount,
        NumberExpression<BigDecimal> reviewRate) {}
}
