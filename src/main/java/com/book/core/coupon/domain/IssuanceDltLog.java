package com.book.core.coupon.domain;

public record IssuanceDltLog(String topic, int partition, long offset, String messageKey, String payload, String errorType) {
}
