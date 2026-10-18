package com.book.core.agentchat.api.response;

import java.math.BigDecimal;

public record AgentCartItemResponse(Long productId, String operation, Integer quantity, int expectedQuantity,
    BigDecimal expectedUnitPrice) {
}
