package com.book.core.review.application.command;

import java.math.BigDecimal;

public record CreateReviewCommand(Long userId,Long orderItemId,BigDecimal rating,String content,boolean isSpoiler){}
