package com.book.core.agentchat.application.result;

import com.book.core.recommendation.application.result.ChatRecommendationResult;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ChatResponseSnapshot(int schemaVersion, Map<String, Object> spec, String reply, List<ChatCardSnapshot> cards, String followup,
    List<String> buttons, boolean degraded, String route, List<AgentButton> agentButtons, AgentExecutionSummary execution) {
    public ChatResponseSnapshot {
        if ((schemaVersion != 1 && schemaVersion != 2) || spec == null || reply == null || cards == null || buttons == null) {
            throw new IllegalArgumentException("Unsupported or incomplete chat response");
        }
        spec = Collections.unmodifiableMap(new LinkedHashMap<>(spec));
        cards = List.copyOf(cards);
        buttons = List.copyOf(buttons);
        if (agentButtons == null) {
            agentButtons = List.of();
        }
        agentButtons = List.copyOf(agentButtons);
        if (schemaVersion == 2 && !("recommend".equals(route) || "shop".equals(route))) {
            throw new IllegalArgumentException("Unsupported agent route");
        }
    }

    public static ChatResponseSnapshot from(final ChatRecommendationResult result) {
        return new ChatResponseSnapshot(1, result.spec(), result.reply(), result.cards().stream().map(ChatCardSnapshot::from).toList(),
            result.followup(), result.buttons(), result.degraded(), null, List.of(), null);
    }

    public static ChatResponseSnapshot agentRecommendation(final ChatRecommendationResult result, final List<AgentButton> buttons) {
        return new ChatResponseSnapshot(2, result.spec(), result.reply(), result.cards().stream().map(ChatCardSnapshot::from).toList(),
            result.followup(), List.of(), result.degraded(), "recommend", buttons, null);
    }

    public static ChatResponseSnapshot shop(final Map<String, Object> spec, final String reply, final List<AgentButton> buttons,
        final AgentExecutionSummary execution) {
        return new ChatResponseSnapshot(2, spec, reply, List.of(), null, List.of(), false, "shop", buttons, execution);
    }

    public String conversationText() {
        if (followup == null || followup.isBlank()) {
            return reply;
        }
        return reply + "\n" + followup;
    }
}
