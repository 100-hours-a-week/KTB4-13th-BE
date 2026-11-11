package com.book.core.onboarding.infrastructure.persistence.repository;

import static com.book.core.book.domain.QBook.book;
import static com.book.core.library.domain.QUserLibraryBook.userLibraryBook;
import static com.book.core.order.domain.QOrder.order;
import static com.book.core.order.domain.QOrderItem.orderItem;
import static com.book.core.product.domain.QProduct.product;
import static com.book.core.review.domain.QReview.review;

import com.book.core.book.application.usecase.GetAiBookIdentifiersUseCase;
import com.book.core.book.domain.QBook;
import com.book.core.onboarding.application.port.AiPersonalizationProfileHistory;
import com.book.core.onboarding.application.result.OwnedBook;
import com.book.core.onboarding.application.port.ProfileHistoryRepositoryPort;
import com.book.core.order.domain.OrderItemStatus;
import com.book.core.order.domain.OrderStatus;
import com.book.core.review.domain.enums.ReviewTargetType;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class ProfileHistoryRepositoryAdapter implements ProfileHistoryRepositoryPort {
    private static final List<OrderStatus> PURCHASED_ORDER_STATUSES = List.of(OrderStatus.PAID, OrderStatus.PARTIAL_CANCELED);
    private static final List<OrderItemStatus> PURCHASED_ITEM_STATUSES = List.of(OrderItemStatus.PAID, OrderItemStatus.PARTIAL_CANCELED);

    private final JPAQueryFactory queryFactory;

    @Override
    public AiPersonalizationProfileHistory findHistory(final Long userId) {
        final List<OwnedBook> ownedBooks = findOwnedBooks(userId);
        final List<String> purchasedIsbns = ownedBooks.stream().filter(OwnedBook::purchased).map(OwnedBook::isbn).sorted().toList();
        final List<String> libraryIsbns =
            ownedBooks.stream().filter((final OwnedBook entry) -> !entry.purchased()).map(OwnedBook::isbn).sorted().toList();
        final List<AiPersonalizationProfileHistory.Review> reviews = queryFactory.select(book.isbn13, review.rate, review.content)
            .from(review).join(product).on(product.id.eq(review.targetId)).join(product.book, book)
            .where(review.userId.eq(userId), review.deletedAt.isNull(), review.targetType.eq(ReviewTargetType.PRODUCT),
                hasUnambiguousIsbn())
            .orderBy(review.id.asc()).fetch().stream()
            .filter((final Tuple row) -> GetAiBookIdentifiersUseCase.isIsbn13(row.get(book.isbn13)))
            .map((final Tuple row) -> new AiPersonalizationProfileHistory.Review(row.get(book.isbn13), row.get(review.rate),
                row.get(review.content)))
            .toList();
        return new AiPersonalizationProfileHistory(purchasedIsbns, libraryIsbns, reviews);
    }

    @Override
    public List<OwnedBook> findOwnedBooks(final Long userId) {
        final var lastPaidAt = order.paidAt.max();
        final List<Tuple> purchased = queryFactory.select(book.isbn13, lastPaidAt).from(orderItem).join(orderItem.order, order)
            .join(product).on(product.id.eq(orderItem.productId)).join(product.book, book)
            .where(order.userId.eq(userId), order.deletedAt.isNull(), order.paidAt.isNotNull(), order.status.in(PURCHASED_ORDER_STATUSES),
                orderItem.deletedAt.isNull(), orderItem.status.in(PURCHASED_ITEM_STATUSES), hasUnambiguousIsbn())
            .groupBy(book.isbn13).fetch();
        final var lastAddedAt = userLibraryBook.addedAt.max();
        final List<Tuple> library =
            queryFactory.select(book.isbn13, lastAddedAt).from(userLibraryBook).join(book).on(book.id.eq(userLibraryBook.bookId))
                .where(userLibraryBook.userId.eq(userId), userLibraryBook.deletedAt.isNull(), hasUnambiguousIsbn()).groupBy(book.isbn13)
                .fetch();
        return Stream
            .concat(purchased.stream().map((final Tuple row) -> new OwnedBook(row.get(book.isbn13), row.get(lastPaidAt), true)),
                library.stream().map((final Tuple row) -> new OwnedBook(row.get(book.isbn13), row.get(lastAddedAt), false)))
            .filter((final OwnedBook entry) -> GetAiBookIdentifiersUseCase.isIsbn13(entry.isbn()))
            .sorted(Comparator.comparing(OwnedBook::acquiredAt).reversed().thenComparing(OwnedBook::isbn)).toList();
    }

    private static BooleanExpression hasUnambiguousIsbn() {
        final QBook other = new QBook("otherBook");
        return book.isbn13.isNotNull()
            .and(JPAExpressions.selectOne().from(other).where(other.isbn13.eq(book.isbn13), other.id.ne(book.id)).notExists());
    }

    @Override
    public List<Long> findChangedUserIds(final Instant from, final Instant before) {
        final LocalDateTime start = LocalDateTime.ofInstant(from, ZoneId.systemDefault());
        final LocalDateTime end = LocalDateTime.ofInstant(before, ZoneId.systemDefault());
        final List<Long> orderUserIds = queryFactory.select(order.userId).distinct().from(order)
            .where(order.paidAt.isNotNull(), order.updatedAt.goe(start), order.updatedAt.lt(end)).fetch();
        final List<Long> orderItemUserIds = queryFactory.select(order.userId).distinct().from(orderItem).join(orderItem.order, order)
            .where(order.paidAt.isNotNull(), orderItem.updatedAt.goe(start), orderItem.updatedAt.lt(end)).fetch();
        final List<Long> libraryUserIds = queryFactory.select(userLibraryBook.userId).distinct().from(userLibraryBook)
            .where(userLibraryBook.updatedAt.goe(start), userLibraryBook.updatedAt.lt(end)).fetch();
        final List<Long> reviewUserIds =
            queryFactory.select(review.userId).distinct().from(review).where(review.updatedAt.goe(start), review.updatedAt.lt(end)).fetch();
        return List.copyOf(Stream.of(orderUserIds, orderItemUserIds, libraryUserIds, reviewUserIds).flatMap(List::stream)
            .collect(TreeSet::new, TreeSet::add, TreeSet::addAll));
    }

}
