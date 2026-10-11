package com.book.core.review.api.response;

import com.book.core.review.domain.enums.ReviewTargetType;
import java.math.BigDecimal;

public record ReviewResponse(Long id,ReviewTargetType targetType,Long targetId,BigDecimal rate,String content){}
