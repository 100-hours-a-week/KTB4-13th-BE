package com.book.core.recommendation.infrastructure.client.ai;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
class AiChatCard {
    private final Long bookId;
    private final Integer matchScore;
    private final String reasonShort;
    private final String reasonLong;

    @JsonCreator
    AiChatCard(@JsonProperty("book_id") final Long bookId, @JsonProperty("match_score") final Integer matchScore,
        @JsonProperty("reason_short") final String reasonShort, @JsonProperty("reason_long") final String reasonLong) {
        this.bookId = bookId;
        this.matchScore = matchScore;
        this.reasonShort = reasonShort;
        this.reasonLong = reasonLong;
    }

    Long bookId() {
        return bookId;
    }

    Integer matchScore() {
        return matchScore;
    }

    String reasonShort() {
        return reasonShort;
    }

    String reasonLong() {
        return reasonLong;
    }
}
