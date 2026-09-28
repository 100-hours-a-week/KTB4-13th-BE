package com.book.core.recommendation.application.port;

public record RecommendationFeedRequest(Long userId,int size,String cursor){}
