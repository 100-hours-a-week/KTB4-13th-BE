package com.book.core.payment.application.command;

public record FailPaymentCommand(String orderKey, String code, String message) {
}
