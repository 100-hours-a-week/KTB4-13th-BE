package com.book.core.recommendation.api.response;

import com.book.core.recommendation.application.port.RecommendationFeedItem;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class RecommendationFeedItemResponse {
    private final Long bookId;
    private final String title;
    private final String author;
    private final BigDecimal price;
    private final String coverUrl;
    private final boolean inStock;
    private final int matchScore;

    public RecommendationFeedItemResponse(final Long bookId, final String title, final String author, final BigDecimal price,
        final String coverUrl, final boolean inStock, final int matchScore) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.price = price;
        this.coverUrl = coverUrl;
        this.inStock = inStock;
        this.matchScore = matchScore;
    }

    public static RecommendationFeedItemResponse from(final RecommendationFeedItem item) {
        return new RecommendationFeedItemResponse(item.bookId(), item.title(), item.author(), item.price(), item.coverUrl(), item.inStock(),
            item.matchScore());
    }

    @JsonProperty
    public Long bookId() {
        return bookId;
    }

    @JsonProperty
    public String title() {
        return title;
    }

    @JsonProperty
    public String author() {
        return author;
    }

    @JsonProperty
    public BigDecimal price() {
        return price;
    }

    @JsonProperty
    public String coverUrl() {
        return coverUrl;
    }

    @JsonProperty
    public boolean inStock() {
        return inStock;
    }

    @JsonProperty
    public int matchScore() {
        return matchScore;
    }
}
