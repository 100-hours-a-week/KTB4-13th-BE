package com.book.core.book.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.book.application.port.NationalLibraryIsbnClient;
import com.book.core.book.domain.Isbn13;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
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
            for (int pageNumber = 1;; pageNumber++) {
                calls = pageNumber;
                final var page = client.findPaperBookIsbns(from, through, pageNumber);
                page.candidateIsbns().stream().map(Isbn13::normalize).flatMap(Optional::stream).forEachOrdered(candidateIsbns::add);
                if (!page.hasNext()) {
                    break;
                }
            }
        } finally {
            final int candidateCount = candidateIsbns.size();
            log.atInfo().addKeyValue("listCallCount", calls).addKeyValue("candidateCount", candidateCount)
                .log("[BOOK] 신간 후보 ISBN 수집이 종료됐습니다. listCallCount={}, candidateCount={}", calls, candidateCount);
        }
        return List.copyOf(candidateIsbns);
    }
}
