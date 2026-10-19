package com.book.core.agentchat.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

record AgentAiOffer(String isbn, String title, BigDecimal price, @JsonProperty("in_stock") boolean inStock,
    @JsonProperty("stock_quantity") int stockQuantity) {
}
