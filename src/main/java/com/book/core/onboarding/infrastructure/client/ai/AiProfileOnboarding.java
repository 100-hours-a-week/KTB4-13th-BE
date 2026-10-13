package com.book.core.onboarding.infrastructure.client.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

record AiProfileOnboarding(@JsonProperty("reading_times") List<String> readingTimes, @JsonProperty("criteria") List<String> criteria,
    @JsonProperty("categories") List<String> categories, @JsonProperty("tags") List<String> tags,
    @JsonProperty("liked_book_ids") List<Long> likedBookIds) {
}
