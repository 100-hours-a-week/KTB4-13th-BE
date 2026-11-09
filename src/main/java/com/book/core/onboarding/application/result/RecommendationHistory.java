package com.book.core.onboarding.application.result;

import java.util.List;

public record RecommendationHistory(boolean consented, List<OwnedBook> books) {
    public static final RecommendationHistory NOT_CONSENTED = new RecommendationHistory(false, List.of());
}
