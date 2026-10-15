package com.book.core.payment.domain.enums;



/** 참고 결제의 TransactionType 계약을 유지합니다. */
public enum TransactionType {
    PAYMENT,
    PAYMENT_FAIL,
    CANCEL,
    PARTIAL_CANCEL
}
