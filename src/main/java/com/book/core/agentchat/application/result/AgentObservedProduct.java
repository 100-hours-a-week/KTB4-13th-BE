package com.book.core.agentchat.application.result;

import java.math.BigDecimal;

public record AgentObservedProduct(Long productId, String isbn, String title, BigDecimal price, int quantity, int stockQuantity,
    boolean active) {
}
