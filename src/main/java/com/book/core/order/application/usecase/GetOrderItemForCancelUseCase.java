package com.book.core.order.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.domain.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/** 참고 취소의 주문상품 ID 조회를 유지하며 주문 소속도 함께 읽습니다. */
@UseCase
@RequiredArgsConstructor
public class GetOrderItemForCancelUseCase {
    private final OrderRepositoryPort orderRepository;

    /** 원본처럼 ID로 주문상품과 소속 주문을 조회합니다. 요청 주문과의 일치 여부는 호출자가 검증합니다. */
    @Transactional(readOnly = true)
    public OrderItem execute(final Long orderItemId) {
        return orderRepository.findOrderItemById(orderItemId).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
    }
}
