package com.book.core.onboarding.api.response;

import com.book.core.onboarding.application.result.OnboardingBookCandidateResult;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OnboardingBookCandidateResponse {
    private final Long bookId;
    private final String title;
    private final String author;
    private final String coverImageUrl;

    public OnboardingBookCandidateResponse(final Long bookId, final String title, final String author, final String coverImageUrl) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.coverImageUrl = coverImageUrl;
    }

    public static OnboardingBookCandidateResponse from(final OnboardingBookCandidateResult result) {
        return new OnboardingBookCandidateResponse(result.bookId(), result.title(), result.author(), result.coverImageUrl());
    }

    @JsonProperty
    public Long bookId() {
        return bookId;
    }

    @JsonProperty
    public String title() {
        return title;
    }

    @JsonProperty
    public String author() {
        return author;
    }

    @JsonProperty
    public String coverImageUrl() {
        return coverImageUrl;
    }
}
