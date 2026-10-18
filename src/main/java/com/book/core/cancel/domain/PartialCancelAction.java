package com.book.core.cancel.domain;

public record PartialCancelAction(String orderKey, Long orderItemId, long quantity) {
}
