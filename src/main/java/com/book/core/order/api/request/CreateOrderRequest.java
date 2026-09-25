package com.book.core.order.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record CreateOrderRequest(
        @Schema(description = "회원 소유의 활성 배송지 ID", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull @Positive
        Long addressId,

        @Schema(description = "장바구니에 담긴 상품과 주문 수량", requiredMode = Schema.RequiredMode.REQUIRED) @NotEmpty
        List<@Valid CreateOrderItemRequest> items) {}
