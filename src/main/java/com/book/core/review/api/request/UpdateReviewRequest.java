package com.book.core.review.api.request;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.review.domain.ReviewContent;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateReviewRequest(@NotNull @DecimalMin("0.5") @DecimalMax("5.0") BigDecimal rate, @NotBlank @Size(max = 255) String content,
    Boolean isSpoiler) {
    public ReviewContent toContent() {
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (rate.compareTo(BigDecimal.valueOf(5)) > 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (content.isEmpty()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        return new ReviewContent(rate, content);
    }
}
