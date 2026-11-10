package com.book.core.recommendation.application.result;

import com.book.core.onboarding.application.result.OwnedBook;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public record RecommendationExclusion(String isbn, RecommendationExclusionSource source) {
    public static List<RecommendationExclusion> newestFirst(final List<String> conversationNewestFirst,
        final List<OwnedBook> ownedBooksNewestFirst) {
        final Set<String> seen = new HashSet<>();
        return Stream
            .concat(
                conversationNewestFirst.stream()
                    .map((final String isbn) -> new RecommendationExclusion(isbn, RecommendationExclusionSource.CONVERSATION)),
                ownedBooksNewestFirst.stream().map(RecommendationExclusion::fromOwnedBook))
            .filter((final RecommendationExclusion entry) -> seen.add(entry.isbn())).toList();
    }

    public static List<String> isbnsFrom(final List<RecommendationExclusion> exclusions, final RecommendationExclusionSource source) {
        return exclusions.stream().filter((final RecommendationExclusion entry) -> entry.source() == source)
            .map(RecommendationExclusion::isbn).toList();
    }

    private static RecommendationExclusion fromOwnedBook(final OwnedBook book) {
        if (book.purchased()) {
            return new RecommendationExclusion(book.isbn(), RecommendationExclusionSource.PURCHASE);
        }
        return new RecommendationExclusion(book.isbn(), RecommendationExclusionSource.LIBRARY);
    }
}
