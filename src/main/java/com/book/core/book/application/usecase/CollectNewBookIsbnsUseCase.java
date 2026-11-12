package com.book.core.book.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.book.application.port.NationalLibraryIsbnClient;
import com.book.core.book.domain.Isbn13;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class CollectNewBookIsbnsUseCase {
    private final NationalLibraryIsbnClient client;

    public List<String> collect(final LocalDate today) {
        final LocalDate from = today.minusDays(30);
        final LocalDate through = today.minusDays(1);
        final var candidateIsbns = new LinkedHashSet<String>();
        int calls = 0;
        try {
            boolean hasNext;
            do {
                final var page = client.findPaperBookIsbns(from, through, ++calls);
                for (final String isbn : page.candidateIsbns()) {
                    Isbn13.normalize(isbn).ifPresent(candidateIsbns::add);
                }
                hasNext = page.hasNext();
            } while (hasNext);
            return List.copyOf(candidateIsbns);
        } finally {
            final int candidateCount = candidateIsbns.size();
            log.atInfo().addKeyValue("listCallCount", calls).addKeyValue("candidateCount", candidateCount)
                .log("[BOOK] 신간 후보 ISBN 수집이 종료됐습니다. listCallCount={}, candidateCount={}", calls, candidateCount);
        }
    }
}
