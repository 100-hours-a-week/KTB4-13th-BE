package com.book.core.agentchat.infrastructure.client;

import java.math.BigDecimal;

record AgentAiAction(String op, String isbn, BigDecimal quantity) {
}
