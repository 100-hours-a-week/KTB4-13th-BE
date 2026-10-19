package com.book.core.agentchat.infrastructure.client;

import com.book.core.agentchat.application.result.AgentChosenBook;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record AgentAiSelection(List<AgentChosenBook> chosen, AgentAiConstraints constraints) {
}
