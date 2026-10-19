package com.book.core.product.application.result;

import java.math.BigDecimal;

/** 장바구니 변경을 검증할 현재 상품 값이며, active는 재고 수량과 별개의 판매 대상 활성 여부입니다. */
public record CartChangeProductResult(Long productId, BigDecimal unitPrice, int stockQuantity, boolean active) {
}
