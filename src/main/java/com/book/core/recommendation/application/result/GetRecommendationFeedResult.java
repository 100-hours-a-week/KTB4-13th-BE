package com.book.core.recommendation.application.result;

import java.util.List;

public record GetRecommendationFeedResult(List<RecommendationFeedItemResult>items,String nextCursor,boolean coldStart,String degraded){}
