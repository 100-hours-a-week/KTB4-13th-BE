package com.book.core.payment.domain;

import java.math.BigDecimal;

public record PaymentItemPrice(Long orderItemId, BigDecimal unitPrice, int quantity) {
}
