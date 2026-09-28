package com.book.core.onboarding.application.result;

public class OnboardingBookCandidateResult {
    private final Long bookId;
    private final String title;
    private final String author;
    private final String coverImageUrl;

    public OnboardingBookCandidateResult(final Long bookId, final String title, final String author, final String coverImageUrl) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.coverImageUrl = coverImageUrl;
    }

    public Long bookId() {
        return bookId;
    }

    public String title() {
        return title;
    }

    public String author() {
        return author;
    }

    public String coverImageUrl() {
        return coverImageUrl;
    }
}
