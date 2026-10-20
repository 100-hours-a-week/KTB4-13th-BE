package com.book.core.agentchat.infrastructure.client;

import java.math.BigDecimal;

record AgentAiCart(String isbn, String title, BigDecimal price, int quantity, boolean available) {
}
