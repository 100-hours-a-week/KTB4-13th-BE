package com.book.core.recommendation.api.response;

import com.book.core.recommendation.application.result.RecommendationCardResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.Objects;

public class RecommendationCardResponse {
    private final Long recommendationCardId;
    private final Long bookId;
    private final Long productId;
    private final String title;
    private final String author;
    private final String coverImageUrl;
    private final BigDecimal price;
    private final Integer matchScore;
    private final String reasonShort;
    private final String reasonLong;

    public RecommendationCardResponse(final Long recommendationCardId, final Long bookId, final Long productId, final String title,
        final String author, final String coverImageUrl, final BigDecimal price, final Integer matchScore, final String reasonShort,
        final String reasonLong) {
        this.recommendationCardId = recommendationCardId;
        this.bookId = bookId;
        this.productId = productId;
        this.title = title;
        this.author = author;
        this.coverImageUrl = coverImageUrl;
        this.price = price;
        this.matchScore = matchScore;
        this.reasonShort = reasonShort;
        this.reasonLong = reasonLong;
    }

    public static RecommendationCardResponse from(final RecommendationCardResult result) {
        return new RecommendationCardResponse(result.recommendationCardId(), result.bookId(), result.productId(), result.title(),
            result.author(), result.coverImageUrl(), result.price(), result.matchScore(), result.reasonShort(), result.reasonLong());
    }

    @JsonProperty("recommendationCardId")
    public Long recommendationCardId() {
        return recommendationCardId;
    }

    @JsonProperty("bookId")
    public Long bookId() {
        return bookId;
    }

    @JsonProperty("productId")
    public Long productId() {
        return productId;
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

    @JsonProperty("price")
    public BigDecimal price() {
        return price;
    }

    @JsonProperty("matchScore")
    public Integer matchScore() {
        return matchScore;
    }

    @JsonProperty("reasonShort")
    public String reasonShort() {
        return reasonShort;
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
            && Objects.equals(productId, that.productId) && Objects.equals(title, that.title) && Objects.equals(author, that.author)
            && Objects.equals(coverImageUrl, that.coverImageUrl) && Objects.equals(price, that.price)
            && Objects.equals(matchScore, that.matchScore) && Objects.equals(reasonShort, that.reasonShort)
            && Objects.equals(reasonLong, that.reasonLong);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recommendationCardId, bookId, productId, title, author, coverImageUrl, price, matchScore, reasonShort,
            reasonLong);
    }
}
