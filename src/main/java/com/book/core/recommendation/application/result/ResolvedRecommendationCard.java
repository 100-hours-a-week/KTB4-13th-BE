package com.book.core.recommendation.application.result;

public record ResolvedRecommendationCard(Long bookId,String title,String author,String coverImageUrl,String reasonLong){}
