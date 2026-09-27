package com.book.core.recommendation.api.response;

import com.book.core.recommendation.application.result.ChatRecommendationResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ChatRecommendationResponse {
    private final Map<String, Object> spec;
    private final String reply;
    private final List<RecommendationCardResponse> cards;
    private final String followup;
    private final List<String> buttons;
    private final boolean degraded;

    public ChatRecommendationResponse(final Map<String, Object> spec, final String reply, final List<RecommendationCardResponse> cards,
        final String followup, final List<String> buttons, final boolean degraded) {
        this.spec = spec;
        this.reply = reply;
        this.cards = cards;
        this.followup = followup;
        this.buttons = buttons;
        this.degraded = degraded;
    }

    public static ChatRecommendationResponse from(final ChatRecommendationResult result) {
        return new ChatRecommendationResponse(result.spec(), result.reply(),
            result.cards().stream().map(RecommendationCardResponse::from).toList(), result.followup(), result.buttons(), result.degraded());
    }

    @JsonProperty("spec")
    public Map<String, Object> spec() {
        return spec;
    }

    @JsonProperty("reply")
    public String reply() {
        return reply;
    }

    @JsonProperty("cards")
    public List<RecommendationCardResponse> cards() {
        return cards;
    }

    @JsonProperty("followup")
    public String followup() {
        return followup;
    }

    @JsonProperty("buttons")
    public List<String> buttons() {
        return buttons;
    }

    @JsonProperty("degraded")
    public boolean degraded() {
        return degraded;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof final ChatRecommendationResponse that)) {
            return false;
        }
        return degraded == that.degraded && Objects.equals(spec, that.spec) && Objects.equals(reply, that.reply)
            && Objects.equals(cards, that.cards) && Objects.equals(followup, that.followup) && Objects.equals(buttons, that.buttons);
    }

    @Override
    public int hashCode() {
        return Objects.hash(spec, reply, cards, followup, buttons, degraded);
    }
}
