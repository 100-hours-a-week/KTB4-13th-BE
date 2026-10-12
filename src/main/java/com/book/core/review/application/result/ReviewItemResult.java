package com.book.core.review.application.result;

import com.book.core.review.domain.enums.ReviewTargetType;
import java.math.BigDecimal;

public record ReviewItemResult(Long id, ReviewTargetType targetType, Long targetId, BigDecimal rate, String content) {
}
