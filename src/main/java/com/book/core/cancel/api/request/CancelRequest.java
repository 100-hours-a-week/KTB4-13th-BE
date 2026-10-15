package com.book.core.cancel.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 전체 취소 요청의 주문 키입니다. */
public record CancelRequest(@NotBlank @Size(max = 255) String orderKey) {
}
