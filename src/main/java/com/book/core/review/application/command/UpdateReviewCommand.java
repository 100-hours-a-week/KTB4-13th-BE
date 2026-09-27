package com.book.core.review.application.command;

import java.math.BigDecimal;

public record UpdateReviewCommand(Long reviewId,BigDecimal rating,String content,Boolean isSpoiler){}
