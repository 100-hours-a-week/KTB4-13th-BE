package com.book.core.agentchat.application.result;

import java.math.BigDecimal;

public record AgentChosenBook(String isbn, BigDecimal price) {
}
