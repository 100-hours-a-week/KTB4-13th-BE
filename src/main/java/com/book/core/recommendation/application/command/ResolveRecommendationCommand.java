package com.book.core.recommendation.application.command;

import java.util.List;
import java.util.Map;

public record ResolveRecommendationCommand(Map<String, Object> spec, String reply, List<RecommendationCardCandidate> cards, String followup,
    List<String> buttons, boolean degraded) {
}
