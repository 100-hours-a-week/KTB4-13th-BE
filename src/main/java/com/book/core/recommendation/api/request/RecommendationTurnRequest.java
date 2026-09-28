package com.book.core.recommendation.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecommendationTurnRequest(String role,@NotBlank @Size(max=200)String text){}
