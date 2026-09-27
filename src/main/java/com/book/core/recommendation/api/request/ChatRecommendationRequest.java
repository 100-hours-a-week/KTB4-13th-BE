package com.book.core.recommendation.api.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChatRecommendationRequest {
    private static final Set<String> REQUIRED_SPEC_KEYS = Set.of("intent", "exact", "filters", "semantic", "anchor_book", "exclude");

    @NotNull
    private final Boolean consented;
    @NotNull
    private final Map<String, Object> spec;
    @NotBlank
    @Size(max = 200)
    private final String message;
    @NotNull
    @Size(max = 20)
    private final List<@Valid RecommendationTurnRequest> recentTurns;
    private final List<Long> excludeBookIds;

    @JsonCreator
    public ChatRecommendationRequest(@JsonProperty("consented") final Boolean consented,
        @JsonProperty("spec") final Map<String, Object> spec, @JsonProperty("message") final String message,
        @JsonProperty("recentTurns") final List<RecommendationTurnRequest> recentTurns,
        @JsonProperty("excludeBookIds") final List<Long> excludeBookIds) {
        this.consented = consented;
        this.spec = spec;
        this.message = message;
        this.recentTurns = recentTurns;
        this.excludeBookIds = excludeBookIds;
    }

    public Boolean consented() {
        return consented;
    }

    public Map<String, Object> spec() {
        return spec;
    }

    public String message() {
        return message;
    }

    public List<RecommendationTurnRequest> recentTurns() {
        return recentTurns;
    }

    public List<Long> excludeBookIds() {
        return excludeBookIds;
    }

    @AssertTrue(message = "spec은 intent, exact, filters, semantic, anchor_book, exclude 6개 key를 모두 포함해야 합니다.")
    public boolean isSpecValid() {
        return spec != null && spec.keySet().containsAll(REQUIRED_SPEC_KEYS);
    }

    public List<Long> excludeBookIdsOrEmpty() {
        return excludeBookIds == null ? List.of() : excludeBookIds;
    }
}
