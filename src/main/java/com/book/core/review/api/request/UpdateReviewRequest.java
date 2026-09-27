package com.book.core.review.api.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateReviewRequest(@DecimalMin("0.0")@DecimalMax("10.0")BigDecimal rating,@Size(max=255)String content,Boolean isSpoiler){}
