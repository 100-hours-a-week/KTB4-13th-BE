package com.book.core.recommendation.application.result;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ChatRecommendationAiOutcome {
    private final Map<String, Object> spec;
    private final String reply;
    private final List<ResolvedRecommendationCard> cards;
    private final String followup;
    private final List<String> buttons;
    private final boolean degraded;

    public ChatRecommendationAiOutcome(final Map<String, Object> spec, final String reply, final List<ResolvedRecommendationCard> cards,
        final String followup, final List<String> buttons, final boolean degraded) {
        this.spec = spec;
        this.reply = reply;
        this.cards = cards;
        this.followup = followup;
        this.buttons = buttons;
        this.degraded = degraded;
    }

    public Map<String, Object> spec() {
        return spec;
    }

    public String reply() {
        return reply;
    }

    public List<ResolvedRecommendationCard> cards() {
        return cards;
    }

    public String followup() {
        return followup;
    }

    public List<String> buttons() {
        return buttons;
    }

    public boolean degraded() {
        return degraded;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof final ChatRecommendationAiOutcome that)) {
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
