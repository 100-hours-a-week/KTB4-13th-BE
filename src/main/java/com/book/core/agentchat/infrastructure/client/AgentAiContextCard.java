package com.book.core.agentchat.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

record AgentAiContextCard(String isbn, int rank, @JsonProperty("match_score") Integer matchScore) {
}
