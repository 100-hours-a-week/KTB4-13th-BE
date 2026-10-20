package com.book.core.review.infrastructure.persistence.repository;

import com.book.core.order.domain.OrderItemStatus;
import com.book.core.review.application.port.ReviewOrderItemRepositoryPort;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.book.core.order.domain.QOrder.order;
import static com.book.core.order.domain.QOrderItem.orderItem;

@Repository
@RequiredArgsConstructor
class ReviewOrderItemRepositoryAdapter implements ReviewOrderItemRepositoryPort {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<Long> findConfirmedOrderItemsForProduct(final Long userId, final Long productId) {
        return queryFactory
            .select(orderItem.id).from(orderItem).join(orderItem.order, order).where(order.deletedAt.isNull(), order.userId.eq(userId),
                orderItem.status.eq(OrderItemStatus.CONFIRMED), orderItem.deletedAt.isNull(), orderItem.productId.eq(productId))
            .orderBy(orderItem.id.asc()).fetch();
    }
}
