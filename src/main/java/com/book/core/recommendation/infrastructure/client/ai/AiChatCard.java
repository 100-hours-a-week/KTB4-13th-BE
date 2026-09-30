package com.book.core.recommendation.infrastructure.client.ai;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
class AiChatCard {
    private final Long bookId;
    private final Integer matchScore;
    private final BigDecimal price;
    private final String reasonShort;
    private final String reasonLong;

    @JsonCreator
    AiChatCard(@JsonProperty("book_id") final Long bookId, @JsonProperty("match_score") final Integer matchScore,
        @JsonProperty("price") final BigDecimal price, @JsonProperty("reason_short") final String reasonShort,
        @JsonProperty("reason_long") final String reasonLong) {
        this.bookId = bookId;
        this.matchScore = matchScore;
        this.price = price;
        this.reasonShort = reasonShort;
        this.reasonLong = reasonLong;
    }

    Long bookId() {
        return bookId;
    }

    Integer matchScore() {
        return matchScore;
    }

    BigDecimal price() {
        return price;
    }

    String reasonShort() {
        return reasonShort;
    }

    String reasonLong() {
        return reasonLong;
    }
}
