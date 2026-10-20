package com.book.core.agentchat.api.response;

import com.book.core.agentchat.application.result.AgentButton;
import com.book.core.agentchat.application.result.AgentExecutionSummary;
import java.util.List;

public record ShoppingChatReplyResponse(String route, String reply, List<AgentButton> buttons,
    AgentExecutionSummary execution) implements ChatReplyResponse {
}
