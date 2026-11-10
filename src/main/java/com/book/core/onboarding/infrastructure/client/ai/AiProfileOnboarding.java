package com.book.core.onboarding.infrastructure.client.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

// @formatter:off
record AiProfileOnboarding(
        @JsonProperty("reading_times") List<String> readingTimes,
        @JsonProperty("criteria") List<String> criteria,
        @JsonProperty("categories") List<String> categories,
        @JsonProperty("tags") List<String> tags,
        @JsonProperty("liked_isbns") List<String> likedIsbns) {}
// @formatter:on
