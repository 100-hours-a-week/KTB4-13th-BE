package com.book.core.recommendation.application.port;

import java.math.BigDecimal;

public class RecommendationFeedItem {
    private final Long bookId;
    private final String title;
    private final String author;
    private final BigDecimal price;
    private final String coverUrl;
    private final boolean inStock;
    private final int matchScore;

    public RecommendationFeedItem(final Long bookId, final String title, final String author, final BigDecimal price, final String coverUrl,
        final boolean inStock, final int matchScore) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.price = price;
        this.coverUrl = coverUrl;
        this.inStock = inStock;
        this.matchScore = matchScore;
    }

    public Long bookId() {
        return bookId;
    }

    public String title() {
        return title;
    }

    public String author() {
        return author;
    }

    public BigDecimal price() {
        return price;
    }

    public String coverUrl() {
        return coverUrl;
    }

    public boolean inStock() {
        return inStock;
    }

    public int matchScore() {
        return matchScore;
    }
}
