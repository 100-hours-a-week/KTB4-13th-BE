package com.book.core.onboarding.infrastructure.client.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

class AiProfileOnboarding {
    private final List<String> readingTimes;
    private final List<String> criteria;
    private final List<String> categories;
    private final List<String> tags;
    private final List<Long> likedBookIds;

    AiProfileOnboarding(final List<String> readingTimes, final List<String> criteria, final List<String> categories,
        final List<String> tags, final List<Long> likedBookIds) {
        this.readingTimes = readingTimes;
        this.criteria = criteria;
        this.categories = categories;
        this.tags = tags;
        this.likedBookIds = likedBookIds;
    }

    @JsonProperty("reading_times")
    List<String> readingTimes() {
        return readingTimes;
    }

    @JsonProperty("criteria")
    List<String> criteria() {
        return criteria;
    }

    @JsonProperty("categories")
    List<String> categories() {
        return categories;
    }

    @JsonProperty("tags")
    List<String> tags() {
        return tags;
    }

    @JsonProperty("liked_book_ids")
    List<Long> likedBookIds() {
        return likedBookIds;
    }
}
