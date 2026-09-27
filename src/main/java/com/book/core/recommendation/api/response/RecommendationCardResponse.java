package com.book.core.recommendation.api.response;

import com.book.core.recommendation.application.result.RecommendationCardResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class RecommendationCardResponse {
    private final Long recommendationCardId;
    private final Long bookId;
    private final String title;
    private final String author;
    private final String coverImageUrl;
    private final String reasonLong;

    public RecommendationCardResponse(final Long recommendationCardId, final Long bookId, final String title, final String author,
        final String coverImageUrl, final String reasonLong) {
        this.recommendationCardId = recommendationCardId;
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.coverImageUrl = coverImageUrl;
        this.reasonLong = reasonLong;
    }

    public static RecommendationCardResponse from(final RecommendationCardResult result) {
        return new RecommendationCardResponse(result.recommendationCardId(), result.bookId(), result.title(), result.author(),
            result.coverImageUrl(), result.reasonLong());
    }

    @JsonProperty("recommendationCardId")
    public Long recommendationCardId() {
        return recommendationCardId;
    }

    @JsonProperty("bookId")
    public Long bookId() {
        return bookId;
    }

    @JsonProperty("title")
    public String title() {
        return title;
    }

    @JsonProperty("author")
    public String author() {
        return author;
    }

    @JsonProperty("coverImageUrl")
    public String coverImageUrl() {
        return coverImageUrl;
    }

    @JsonProperty("reasonLong")
    public String reasonLong() {
        return reasonLong;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof final RecommendationCardResponse that)) {
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
