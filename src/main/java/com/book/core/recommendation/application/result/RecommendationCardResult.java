package com.book.core.recommendation.application.result;

import java.util.Objects;

public class RecommendationCardResult {
    private final Long recommendationCardId;
    private final Long bookId;
    private final String title;
    private final String author;
    private final String coverImageUrl;
    private final String reasonLong;

    public RecommendationCardResult(final Long recommendationCardId, final Long bookId, final String title, final String author,
        final String coverImageUrl, final String reasonLong) {
        this.recommendationCardId = recommendationCardId;
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.coverImageUrl = coverImageUrl;
        this.reasonLong = reasonLong;
    }

    public Long recommendationCardId() {
        return recommendationCardId;
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

    public String coverImageUrl() {
        return coverImageUrl;
    }

    public String reasonLong() {
        return reasonLong;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof final RecommendationCardResult that)) {
            return false;
        }
        return Objects.equals(recommendationCardId, that.recommendationCardId) && Objects.equals(bookId, that.bookId)
            && Objects.equals(title, that.title) && Objects.equals(author, that.author) && Objects.equals(coverImageUrl, that.coverImageUrl)
            && Objects.equals(reasonLong, that.reasonLong);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recommendationCardId, bookId, title, author, coverImageUrl, reasonLong);
    }
}
