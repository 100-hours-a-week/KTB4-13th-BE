package com.book.core.agentchat.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
record AgentAiConstraints(@JsonProperty("budget_total") BigDecimal budgetTotal, Integer count) {
}
