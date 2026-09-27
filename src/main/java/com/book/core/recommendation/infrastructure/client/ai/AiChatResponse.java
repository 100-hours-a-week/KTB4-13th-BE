package com.book.core.recommendation.infrastructure.client.ai;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
class AiChatResponse {
    private final Map<String, Object> spec;
    private final String reply;
    private final List<AiChatCard> cards;
    private final String followup;
    private final List<String> buttons;
    private final boolean degraded;

    @JsonCreator
    AiChatResponse(@JsonProperty("spec") final Map<String, Object> spec, @JsonProperty("reply") final String reply,
        @JsonProperty("cards") final List<AiChatCard> cards, @JsonProperty("followup") final String followup,
        @JsonProperty("buttons") final List<String> buttons, @JsonProperty("degraded") final boolean degraded) {
        this.spec = spec;
        this.reply = reply;
        this.cards = cards;
        this.followup = followup;
        this.buttons = buttons;
        this.degraded = degraded;
    }

    Map<String, Object> spec() {
        return spec;
    }

    String reply() {
        return reply;
    }

    List<AiChatCard> cards() {
        return cards;
    }

    String followup() {
        return followup;
    }

    List<String> buttons() {
        return buttons;
    }

    boolean degraded() {
        return degraded;
    }
}
