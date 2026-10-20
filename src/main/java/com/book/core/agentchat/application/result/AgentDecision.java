package com.book.core.agentchat.application.result;

import com.book.core.recommendation.application.command.ResolveRecommendationCommand;
import java.util.List;

public record AgentDecision(String route, String reply, ResolveRecommendationCommand recommendation, List<AgentAction> actions,
    AgentSelection selection, List<AgentButton> buttons) {
    public AgentDecision {
        actions = List.copyOf(actions);
        buttons = List.copyOf(buttons);
    }
}
