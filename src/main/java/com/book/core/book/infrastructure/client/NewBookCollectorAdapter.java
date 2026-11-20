package com.book.core.book.infrastructure.client;

import com.book.core.book.application.port.NewBookCollector;
import com.book.core.book.application.result.BookDetail;
import com.book.core.book.application.result.NewBookCandidate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class NewBookCollectorAdapter implements NewBookCollector {
    private final NationalLibraryClient nationalLibrary;
    private final JeongboNaruClient jeongboNaru;

    @Override
    public List<NewBookCandidate> collectCandidates(final LocalDate from, final LocalDate through) {
        final var candidates = new ArrayList<NewBookCandidate>();
        int calls = 0;
        try {
            for (int pageNumber = 1;; pageNumber++) {
                calls = pageNumber;
                final NationalLibraryPage page = nationalLibrary.searchPaperBooks(from, through, pageNumber);
                candidates.addAll(page.candidates());
                if (!page.hasNext()) {
                    break;
                }
            }
        } finally {
            final int candidateCount = candidates.size();
            log.atInfo().addKeyValue("listCallCount", calls).addKeyValue("candidateCount", candidateCount)
                .log("[BOOK] 국중도 신간 후보 수집이 종료됐습니다. listCallCount={}, candidateCount={}", calls, candidateCount);
        }
        return List.copyOf(candidates);
    }

    @Override
    public Optional<BookDetail> collectDetail(final String isbn13) {
        return jeongboNaru.findBookDetail(isbn13);
    }
}
