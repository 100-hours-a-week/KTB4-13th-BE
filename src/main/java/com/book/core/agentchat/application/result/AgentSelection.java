package com.book.core.agentchat.application.result;

import java.math.BigDecimal;
import java.util.List;

public record AgentSelection(List<AgentChosenBook> chosen, BigDecimal budgetTotal, Integer count) {
    public AgentSelection {
        chosen = List.copyOf(chosen);
    }
}
