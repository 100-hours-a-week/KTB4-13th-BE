package com.book.core.onboarding.application.port;

import java.util.List;
import java.util.Objects;

public class PersonalizationProfileRequest {
    private final Long userId;
    private final String idempotencyKey;
    private final List<String> readingTimes;
    private final List<String> criteria;
    private final List<String> categories;
    private final List<String> tags;
    private final List<Long> likedBookIds;

    public PersonalizationProfileRequest(final Long userId, final String idempotencyKey, final List<String> readingTimes,
        final List<String> criteria, final List<String> categories, final List<String> tags, final List<Long> likedBookIds) {
        this.userId = userId;
        this.idempotencyKey = idempotencyKey;
        this.readingTimes = readingTimes;
        this.criteria = criteria;
        this.categories = categories;
        this.tags = tags;
        this.likedBookIds = likedBookIds;
    }

    public Long userId() {
        return userId;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public List<String> readingTimes() {
        return readingTimes;
    }

    public List<String> criteria() {
        return criteria;
    }

    public List<String> categories() {
        return categories;
    }

    public List<String> tags() {
        return tags;
    }

    public List<Long> likedBookIds() {
        return likedBookIds;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof final PersonalizationProfileRequest that)) {
            return false;
        }
        return Objects.equals(userId, that.userId) && Objects.equals(idempotencyKey, that.idempotencyKey)
            && Objects.equals(readingTimes, that.readingTimes) && Objects.equals(criteria, that.criteria)
            && Objects.equals(categories, that.categories) && Objects.equals(tags, that.tags)
            && Objects.equals(likedBookIds, that.likedBookIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, idempotencyKey, readingTimes, criteria, categories, tags, likedBookIds);
    }
}
