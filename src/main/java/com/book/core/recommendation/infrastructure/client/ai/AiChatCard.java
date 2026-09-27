package com.book.core.recommendation.infrastructure.client.ai;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
class AiChatCard {
    private final Long bookId;
    private final String reasonLong;

    @JsonCreator
    AiChatCard(@JsonProperty("book_id") final Long bookId, @JsonProperty("reason_long") final String reasonLong) {
        this.bookId = bookId;
        this.reasonLong = reasonLong;
    }

    Long bookId() {
        return bookId;
    }

    String reasonLong() {
        return reasonLong;
    }
}
