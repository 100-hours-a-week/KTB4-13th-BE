package com.book.core.payment.application.command;

import java.math.BigDecimal;

public record ApprovePaymentCommand(String orderKey, String externalPaymentKey, BigDecimal amount) {
}
