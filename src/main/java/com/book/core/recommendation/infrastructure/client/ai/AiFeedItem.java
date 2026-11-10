package com.book.core.recommendation.infrastructure.client.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
record AiFeedItem(String isbn, String title, String author, BigDecimal price, @JsonProperty("cover_url") String coverUrl,
    @JsonProperty("match_score") int matchScore) {
}
