package com.book.core.onboarding.infrastructure.persistence.repository;

import static com.book.core.book.domain.QBook.book;
import static com.book.core.library.domain.QUserLibraryBook.userLibraryBook;
import static com.book.core.order.domain.QOrder.order;
import static com.book.core.order.domain.QOrderItem.orderItem;
import static com.book.core.product.domain.QProduct.product;
import static com.book.core.review.domain.QReview.review;

import com.book.core.onboarding.application.port.AiPersonalizationProfileHistory;
import com.book.core.onboarding.application.port.ProfileHistoryRepositoryPort;
import com.book.core.order.domain.OrderItemStatus;
import com.book.core.order.domain.OrderStatus;
import com.book.core.review.domain.enums.ReviewTargetType;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class ProfileHistoryRepositoryAdapter implements ProfileHistoryRepositoryPort {
    // AI 서버는 형식이 틀린 ISBN이 하나라도 있으면 요청 전체를 거절하므로 13자리 숫자만 보냅니다.
    private static final Pattern ISBN13 = Pattern.compile("\\d{13}");
    private static final List<OrderStatus> PURCHASED_ORDER_STATUSES = List.of(OrderStatus.PAID, OrderStatus.PARTIAL_CANCELED);
    private static final List<OrderItemStatus> PURCHASED_ITEM_STATUSES = List.of(OrderItemStatus.PAID, OrderItemStatus.PARTIAL_CANCELED);

    private final JPAQueryFactory queryFactory;

    @Override
    public AiPersonalizationProfileHistory findHistory(final Long userId) {
        final List<String> purchased =
            queryFactory.select(book.isbn13).distinct().from(orderItem).join(orderItem.order, order).join(product)
                .on(product.id.eq(orderItem.productId)).join(product.book, book).where(order.userId.eq(userId), order.deletedAt.isNull(),
                    order.status.in(PURCHASED_ORDER_STATUSES), orderItem.deletedAt.isNull(), orderItem.status.in(PURCHASED_ITEM_STATUSES))
                .fetch();
        final List<String> library = queryFactory.select(book.isbn13).distinct().from(userLibraryBook).join(book)
            .on(book.id.eq(userLibraryBook.bookId)).where(userLibraryBook.userId.eq(userId), userLibraryBook.deletedAt.isNull()).fetch();
        final List<AiPersonalizationProfileHistory.Review> reviews = queryFactory.select(book.isbn13, review.rate, review.content)
            .from(review).join(product).on(product.id.eq(review.targetId)).join(product.book, book)
            .where(review.userId.eq(userId), review.deletedAt.isNull(), review.targetType.eq(ReviewTargetType.PRODUCT))
            .orderBy(review.id.asc()).fetch().stream().filter((final Tuple row) -> isIsbn13(row.get(book.isbn13)))
            .map((final Tuple row) -> new AiPersonalizationProfileHistory.Review(row.get(book.isbn13), row.get(review.rate),
                row.get(review.content)))
            .toList();
        return new AiPersonalizationProfileHistory(isbns(purchased), isbns(library), reviews);
    }

    @Override
    public List<Long> findChangedUserIds(final Instant from, final Instant before) {
        // updated_at은 JVM 기본 시간대로 저장되므로 같은 시간대로 바꿔 비교합니다.
        final LocalDateTime start = LocalDateTime.ofInstant(from, ZoneId.systemDefault());
        final LocalDateTime end = LocalDateTime.ofInstant(before, ZoneId.systemDefault());
        final List<Long> orders = queryFactory.select(order.userId).distinct().from(order)
            .where(order.paidAt.isNotNull(), order.updatedAt.goe(start), order.updatedAt.lt(end)).fetch();
        final List<Long> orderItems = queryFactory.select(order.userId).distinct().from(orderItem).join(orderItem.order, order)
            .where(order.paidAt.isNotNull(), orderItem.updatedAt.goe(start), orderItem.updatedAt.lt(end)).fetch();
        final List<Long> library = queryFactory.select(userLibraryBook.userId).distinct().from(userLibraryBook)
            .where(userLibraryBook.updatedAt.goe(start), userLibraryBook.updatedAt.lt(end)).fetch();
        final List<Long> reviews =
            queryFactory.select(review.userId).distinct().from(review).where(review.updatedAt.goe(start), review.updatedAt.lt(end)).fetch();
        return List.copyOf(
            Stream.of(orders, orderItems, library, reviews).flatMap(List::stream).collect(TreeSet::new, TreeSet::add, TreeSet::addAll));
    }

    private static List<String> isbns(final List<String> values) {
        return values.stream().filter(ProfileHistoryRepositoryAdapter::isIsbn13).sorted().toList();
    }

    private static boolean isIsbn13(final String value) {
        return value != null && ISBN13.matcher(value).matches();
    }
}
