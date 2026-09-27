package com.book.core.recommendation.application.result;

import com.book.core.book.domain.Book;
import com.book.core.product.domain.Product;
import com.book.core.recommendation.domain.RecommendationCard;
import java.math.BigDecimal;
import java.util.Objects;

public class RecommendationCardDetailResult {
    private final Long recommendationCardId;
    private final Long bookId;
    private final String title;
    private final String author;
    private final String coverImageUrl;
    private final String reasonLong;
    private final Long productId;
    private final BigDecimal price;

    public RecommendationCardDetailResult(final Long recommendationCardId, final Long bookId, final String title, final String author,
        final String coverImageUrl, final String reasonLong, final Long productId, final BigDecimal price) {
        this.recommendationCardId = recommendationCardId;
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.coverImageUrl = coverImageUrl;
        this.reasonLong = reasonLong;
        this.productId = productId;
        this.price = price;
    }

    public static RecommendationCardDetailResult of(final RecommendationCard card, final Book book, final Product product) {
        final Long productId = product == null ? null : product.id();
        final BigDecimal price = product == null ? null : effectivePrice(product);
        return new RecommendationCardDetailResult(card.id(), book.id(), book.title(), book.author(), book.coverImageUrl(),
            card.reasonLong(), productId, price);
    }

    private static BigDecimal effectivePrice(final Product product) {
        if (product.discountedPrice() != null && product.discountedPrice().signum() > 0) {
            return product.discountedPrice();
        }
        return product.salePrice();
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

    public Long productId() {
        return productId;
    }

    public BigDecimal price() {
        return price;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof final RecommendationCardDetailResult that)) {
            return false;
        }
        return Objects.equals(recommendationCardId, that.recommendationCardId) && Objects.equals(bookId, that.bookId)
            && Objects.equals(title, that.title) && Objects.equals(author, that.author) && Objects.equals(coverImageUrl, that.coverImageUrl)
            && Objects.equals(reasonLong, that.reasonLong) && Objects.equals(productId, that.productId)
            && Objects.equals(price, that.price);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recommendationCardId, bookId, title, author, coverImageUrl, reasonLong, productId, price);
    }
}
