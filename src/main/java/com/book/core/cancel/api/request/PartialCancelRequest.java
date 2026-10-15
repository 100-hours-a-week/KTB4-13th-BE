package com.book.core.cancel.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** 부분 취소 요청의 주문 키, 주문상품 ID, 취소 수량입니다. */
public record PartialCancelRequest(@NotBlank @Size(max = 255) String orderKey, @NotNull @Positive Long orderItemId,
    @NotNull @Positive Long quantity) {
}
