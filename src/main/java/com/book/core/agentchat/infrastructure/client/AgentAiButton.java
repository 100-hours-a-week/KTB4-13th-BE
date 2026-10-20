package com.book.core.agentchat.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record AgentAiButton(String label, String action, String target, List<String> isbns) {
}
