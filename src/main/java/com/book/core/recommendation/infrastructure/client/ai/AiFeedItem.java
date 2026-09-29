package com.book.core.recommendation.infrastructure.client.ai;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
class AiFeedItem {
    private final Long bookId;
    private final String title;
    private final String author;
    private final BigDecimal price;
    private final String coverUrl;
    private final boolean inStock;
    private final int matchScore;

    @JsonCreator
    AiFeedItem(@JsonProperty("book_id") final Long bookId, @JsonProperty("title") final String title,
        @JsonProperty("author") final String author, @JsonProperty("price") final BigDecimal price,
        @JsonProperty("cover_url") final String coverUrl, @JsonProperty("in_stock") final boolean inStock,
        @JsonProperty("match_score") final int matchScore) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.price = price;
        this.coverUrl = coverUrl;
        this.inStock = inStock;
        this.matchScore = matchScore;
    }

    Long bookId() {
        return bookId;
    }

    String title() {
        return title;
    }

    String author() {
        return author;
    }

    BigDecimal price() {
        return price;
    }

    String coverUrl() {
        return coverUrl;
    }

    boolean inStock() {
        return inStock;
    }

    int matchScore() {
        return matchScore;
    }
}
