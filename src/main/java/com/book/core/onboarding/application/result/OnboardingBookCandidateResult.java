package com.book.core.onboarding.application.result;

public class OnboardingBookCandidateResult {
    private final Long bookId;
    private final String title;
    private final String author;
    private final String coverImageUrl;
    private final int displayOrder;

    public OnboardingBookCandidateResult(final Long bookId, final String title, final String author, final String coverImageUrl,
        final int displayOrder) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.coverImageUrl = coverImageUrl;
        this.displayOrder = displayOrder;
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

    public int displayOrder() {
        return displayOrder;
    }
}
