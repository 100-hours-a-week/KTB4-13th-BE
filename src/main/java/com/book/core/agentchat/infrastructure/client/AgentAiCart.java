package com.book.core.agentchat.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

record AgentAiCart(String isbn, @JsonProperty("product_id") Long productId, String title, BigDecimal price, int quantity,
    boolean available) {
}
