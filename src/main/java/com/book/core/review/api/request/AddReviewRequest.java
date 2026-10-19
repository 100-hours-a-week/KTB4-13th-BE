package com.book.core.review.api.request;

import com.book.core.review.domain.enums.ReviewTargetType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record AddReviewRequest(@NotNull ReviewTargetType targetType, @NotNull @Positive Long targetId,
    @NotNull @DecimalMin("0.5") @DecimalMax("5.0") BigDecimal rate, @NotBlank @Size(max = 255) String content, @NotNull Boolean isSpoiler) {
}
