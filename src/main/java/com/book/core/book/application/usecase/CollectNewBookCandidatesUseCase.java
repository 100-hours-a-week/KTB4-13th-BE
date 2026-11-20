package com.book.core.book.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.book.application.port.NewBookCollector;
import com.book.core.book.application.result.NewBookCandidate;
import com.book.core.book.domain.Isbn13;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CollectNewBookCandidatesUseCase {
    private final NewBookCollector collector;

    public List<NewBookCandidate> collect(final LocalDate today) {
        final var candidates = new LinkedHashMap<String, NewBookCandidate>();
        for (final NewBookCandidate candidate : collector.collectCandidates(today.minusDays(30), today.minusDays(1))) {
            Isbn13.normalize(candidate.isbn13())
                .ifPresent((final var isbn13) -> candidates.putIfAbsent(isbn13, new NewBookCandidate(isbn13, candidate.publishedAt())));
        }
        return List.copyOf(candidates.values());
    }
}
