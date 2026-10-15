package com.book.core.cancel.domain;



/** 참고 부분 취소의 주문 키, 주문상품 ID, 취소 수량 계약입니다. */
public record PartialCancelAction(String orderKey, Long orderItemId, long quantity) {
}
