package com.book.core.search.infrastructure.client.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

// @formatter:off
@JsonIgnoreProperties(ignoreUnknown = true)
record AiSearchItem(
    @JsonProperty("book_id") Long bookId,
    @JsonProperty("title") String title,
    @JsonProperty("author") String author,
    @JsonProperty("publisher") String publisher,
    @JsonProperty("price") BigDecimal price,
    @JsonProperty("in_stock") boolean inStock,
    @JsonProperty("cover_url") String coverUrl
) {}
// @formatter:on
