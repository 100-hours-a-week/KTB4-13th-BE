package com.book.core.book.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.book.application.port.NewBookIsbnFinder;
import com.book.core.book.application.result.NewBookCandidate;
import com.book.core.book.domain.Isbn13;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class CollectNewBookIsbnsUseCase {
    private final NewBookIsbnFinder isbnFinder;

    public List<NewBookCandidate> collect(final LocalDate today) {
        final LocalDate from = today.minusDays(30);
        final LocalDate through = today.minusDays(1);
        final var candidates = new LinkedHashMap<String, NewBookCandidate>();
        int calls = 0;
        try {
            for (int pageNumber = 1;; pageNumber++) {
                calls = pageNumber;
                final var page = isbnFinder.findPaperBookIsbns(from, through, pageNumber);
                for (final NewBookCandidate candidate : page.candidates()) {
                    Isbn13.normalize(candidate.isbn13()).ifPresent((final var isbn13) -> candidates.putIfAbsent(isbn13,
                        new NewBookCandidate(isbn13, candidate.publishedAt())));
                }
                if (!page.hasNext()) {
                    break;
                }
            }
        } finally {
            final int candidateCount = candidates.size();
            log.atInfo().addKeyValue("listCallCount", calls).addKeyValue("candidateCount", candidateCount)
                .log("[BOOK] 신간 후보 ISBN 수집이 종료됐습니다. listCallCount={}, candidateCount={}", calls, candidateCount);
        }
        return List.copyOf(candidates.values());
    }
}
