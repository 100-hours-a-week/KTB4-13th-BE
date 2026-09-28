package com.book.core.recommendation.application.command;

public record GetRecommendationFeedCommand(Long userId,int size,String cursor){}
