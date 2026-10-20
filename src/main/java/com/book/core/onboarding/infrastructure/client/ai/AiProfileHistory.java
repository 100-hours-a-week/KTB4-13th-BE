package com.book.core.onboarding.infrastructure.client.ai;

import com.book.core.onboarding.application.port.AiPersonalizationProfileHistory;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

// @formatter:off
record AiProfileHistory(
        @JsonProperty("purchased_isbns") List<String> purchasedIsbns,
        @JsonProperty("library_isbns") List<String> libraryIsbns,
        @JsonProperty("reviews") List<AiProfileReview> reviews) {
    static AiProfileHistory from(final AiPersonalizationProfileHistory history) {
        return new AiProfileHistory(history.purchasedIsbns(), history.libraryIsbns(), history.reviews().stream()
                .map((final AiPersonalizationProfileHistory.Review review) -> new AiProfileReview(review.isbn(), review.rating(), review.content())).toList());
    }

    record AiProfileReview(
            @JsonProperty("isbn") String isbn,
            @JsonProperty("rating") BigDecimal rating,
            @JsonProperty("content") String content) {}
}
// @formatter:on
