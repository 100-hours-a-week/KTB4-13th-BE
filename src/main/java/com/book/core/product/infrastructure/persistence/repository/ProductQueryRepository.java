package com.book.core.product.infrastructure.persistence.repository;

import static com.book.core.book.domain.QBook.book;
import static com.book.core.category.domain.QCategory.category;
import static com.book.core.product.domain.QProduct.product;
import static com.book.core.product.domain.QProductCategory.productCategory;

import com.book.core.product.domain.Product;
import com.book.core.product.domain.QProduct;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class ProductQueryRepository {
    private final JPAQueryFactory queryFactory;

    List<Product> findActiveProducts(final Long categoryId, final Long cursor, final int limit) {
        return queryFactory
                .selectFrom(product)
                .join(product.book, book)
                .fetchJoin()
                .where(
                        product.deletedAt.isNull(),
                        book.deletedAt.isNull(),
                        cursorPredicate(cursor),
                        categoryPredicate(categoryId))
                .orderBy(product.createdAt.desc(), product.id.desc())
                .limit(limit)
                .fetch();
    }

    private BooleanExpression cursorPredicate(final Long cursor) {
        if (cursor == null) {
            return null;
        }
        final var cursorProduct = new QProduct("cursorProduct");
        final var cursorCreatedAt = JPAExpressions.select(cursorProduct.createdAt)
                .from(cursorProduct)
                .where(cursorProduct.id.eq(cursor));
        return product.createdAt
                .lt(cursorCreatedAt)
                .or(product.createdAt.eq(cursorCreatedAt).and(product.id.lt(cursor)));
    }

    private BooleanExpression categoryPredicate(final Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return JPAExpressions.selectOne()
                .from(productCategory)
                .join(productCategory.category, category)
                .where(
                        productCategory.product.eq(product),
                        productCategory.deletedAt.isNull(),
                        category.id.eq(categoryId),
                        category.deletedAt.isNull())
                .exists();
    }
}
