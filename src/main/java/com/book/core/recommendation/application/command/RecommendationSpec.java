package com.book.core.recommendation.application.command;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 첫 대화와 외부 AI 조건 초기화가 공유하는 추천 계약입니다. */
public final class RecommendationSpec {
    private RecommendationSpec() {}

    public static Map<String, Object> initial() {
        final Map<String, Object> exact = new HashMap<>();
        exact.put("title", null);
        exact.put("author", null);
        exact.put("publisher", null);
        final Map<String, Object> spec = new HashMap<>();
        spec.put("intent", "semantic");
        spec.put("exact", Collections.unmodifiableMap(exact));
        spec.put("filters", Map.of());
        spec.put("semantic", null);
        spec.put("anchor_book", null);
        spec.put("exclude", List.of());
        return Collections.unmodifiableMap(spec);
    }
}
