package com.book.core.agentchat.application.result;

import com.book.core.recommendation.application.result.ChatRecommendationResult;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ChatResponseSnapshot(int schemaVersion, Map<String, Object> spec, String reply, List<ChatCardSnapshot> cards, String followup,
    List<String> buttons, boolean degraded) {
    public ChatResponseSnapshot {
        if (schemaVersion != 1 || spec == null || reply == null || cards == null || buttons == null) {
            throw new IllegalArgumentException("Unsupported or incomplete chat response");
        }
        spec = Collections.unmodifiableMap(new LinkedHashMap<>(spec));
        cards = List.copyOf(cards);
        buttons = List.copyOf(buttons);

    }

    public static ChatResponseSnapshot from(final ChatRecommendationResult result) {
        return new ChatResponseSnapshot(1, result.spec(), result.reply(), result.cards().stream().map(ChatCardSnapshot::from).toList(),
            result.followup(), result.buttons(), result.degraded());
    }

    public String conversationText() {
        if (followup == null || followup.isBlank()) {
            return reply;
        }
        return reply + "\n" + followup;
    }
}
