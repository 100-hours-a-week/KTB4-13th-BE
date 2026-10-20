package com.book.core.review.infrastructure.persistence.repository;

import com.book.core.order.domain.OrderStatus;
import com.book.core.review.application.port.ReviewOrderItemRepositoryPort;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static com.book.core.order.domain.QOrder.order;
import static com.book.core.order.domain.QOrderItem.orderItem;

@Repository
@RequiredArgsConstructor
class ReviewOrderItemRepositoryAdapter implements ReviewOrderItemRepositoryPort {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<Long> findRecentOrderItemsForProduct(final Long userId, final Long productId, final OrderStatus orderStatus,
        final LocalDateTime fromDate) {
        return queryFactory
            .select(orderItem.id).from(orderItem).join(orderItem.order, order).where(order.deletedAt.isNull(), order.userId.eq(userId),
                order.status.eq(orderStatus), order.paidAt.goe(fromDate), orderItem.deletedAt.isNull(), orderItem.productId.eq(productId))
            .orderBy(orderItem.id.asc()).fetch();
    }
}
