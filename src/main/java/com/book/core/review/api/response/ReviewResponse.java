package com.book.core.review.api.response;

import com.book.core.review.domain.enums.ReviewTargetType;
import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;

public record ReviewResponse(Long id, ReviewTargetType targetType, Long targetId, BigDecimal rate, String content,
    @Schema(description = "작성자가 지정한 스포일러 여부") boolean isSpoiler) {
}
