package com.book.core.review.infrastructure.persistence.repository;

import static com.book.core.order.domain.QOrder.order;
import static com.book.core.order.domain.QOrderItem.orderItem;

import com.book.core.order.domain.OrderItemStatus;
import com.book.core.order.domain.OrderStatus;
import com.book.core.review.application.port.ReviewOrderRepositoryPort;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class ReviewOrderRepositoryAdapter implements ReviewOrderRepositoryPort {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<Long> findRecentPaidOrderItemIds(final Long userId, final Long productId, final LocalDateTime fromDate) {
        return queryFactory.select(orderItem.id).from(orderItem).join(orderItem.order, order)
            .where(order.userId.eq(userId), order.status.eq(OrderStatus.PAID), order.createdAt.goe(fromDate), order.deletedAt.isNull(),
                orderItem.productId.eq(productId), orderItem.status.eq(OrderItemStatus.PAID), orderItem.deletedAt.isNull())
            .orderBy(orderItem.id.asc()).fetch();
    }
}
