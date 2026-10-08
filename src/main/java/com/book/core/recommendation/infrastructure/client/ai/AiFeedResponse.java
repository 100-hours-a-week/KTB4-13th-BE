package com.book.core.recommendation.infrastructure.client.ai;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
class AiFeedResponse {
    private final List<AiFeedItem> items;
    private final String nextCursor;
    private final boolean coldStart;

    @JsonCreator
    AiFeedResponse(@JsonProperty("items") final List<AiFeedItem> items, @JsonProperty("next_cursor") final String nextCursor,
        @JsonProperty("cold_start") final boolean coldStart) {
        this.items = items;
        this.nextCursor = nextCursor;
        this.coldStart = coldStart;
    }

    List<AiFeedItem> items() {
        return items;
    }

    String nextCursor() {
        return nextCursor;
    }

    boolean coldStart() {
        return coldStart;
    }
}
