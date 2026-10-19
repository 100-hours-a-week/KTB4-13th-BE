package com.book.core.product.application.port;

import com.book.core.product.application.result.CartChangeProductResult;
import java.util.List;
import java.util.Map;

/** 장바구니 변경 트랜잭션이 끝날 때까지 상품과 도서의 현재 값을 보호하는 저장 계약입니다. */
public interface CartChangeProductRepositoryPort {
    /** 상품을 일괄 조회하고 상품·도서 ID 순서로 커밋 시 강제 버전 갱신을 등록합니다. 경합하면 전체 롤백합니다. */
    Map<Long, CartChangeProductResult> findByIdsForChange(final List<Long> productIds);
}
