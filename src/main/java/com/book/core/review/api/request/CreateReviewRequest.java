package com.book.core.review.api.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateReviewRequest(@NotNull @Positive Long userId,@NotNull @Positive Long targetId,@NotNull @DecimalMin("0.0")@DecimalMax("10.0")BigDecimal rate,@NotNull @Size(max=255)String content,@NotNull Boolean isSpoiler){}
