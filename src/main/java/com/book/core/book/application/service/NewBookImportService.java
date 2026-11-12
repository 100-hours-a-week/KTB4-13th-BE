package com.book.core.book.application.service;

import com.book.core.book.application.port.DuplicateImportedIsbnException;
import com.book.core.book.application.port.NewBookImportLock;
import com.book.core.book.application.result.NewBookDetailFound;
import com.book.core.book.application.result.NewBookImportOutcome;
import com.book.core.book.application.usecase.CollectNewBookIsbnsUseCase;
import com.book.core.book.application.usecase.ImportNewBookDetailUseCase;
import com.book.core.book.application.usecase.RegisterNewBookUseCase;
import java.time.LocalDate;
import java.util.EnumMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewBookImportService {
    private final NewBookImportLock batchLock;
    private final CollectNewBookIsbnsUseCase collection;
    private final ImportNewBookDetailUseCase detailImport;
    private final RegisterNewBookUseCase registration;

    public void importNewBooks(final LocalDate today) {
        if (!batchLock.runIfAcquired(() -> importCandidates(today))) {
            log.info("[BOOK] 다른 인스턴스가 신간 도서 등록 배치를 실행 중이어서 건너뜁니다.");
        }
    }

    private void importCandidates(final LocalDate today) {
        final long startedAt = System.nanoTime();
        final var outcomes = new EnumMap<NewBookImportOutcome, Integer>(NewBookImportOutcome.class);
        int candidates = 0;
        try {
            final var candidateIsbns = collection.collect(today);
            candidates = candidateIsbns.size();
            for (final String isbn13 : candidateIsbns) {
                final NewBookImportOutcome outcome = importBook(isbn13);
                outcomes.merge(outcome, 1, Integer::sum);
                if (outcome == NewBookImportOutcome.BUDGET_EXHAUSTED || outcome == NewBookImportOutcome.RATE_LIMITED) {
                    break;
                }
            }
        } finally {
            final int detailCalls = outcomes.values().stream().mapToInt(Integer::intValue).sum()
                - outcomes.getOrDefault(NewBookImportOutcome.ALREADY_REGISTERED, 0)
                - outcomes.getOrDefault(NewBookImportOutcome.BUDGET_EXHAUSTED, 0);
            final long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
            log.atInfo().addKeyValue("candidateCount", candidates).addKeyValue("detailCallCount", detailCalls)
                .addKeyValue("outcomes", outcomes).addKeyValue("elapsedMs", elapsedMs)
                .log("[BOOK] 신간 도서 등록 배치가 종료됐습니다. candidateCount={}, detailCallCount={}, outcomes={}, elapsedMs={}", candidates,
                    detailCalls, outcomes, elapsedMs);
        }
    }

    private NewBookImportOutcome importBook(final String isbn13) {
        final long startedAt = System.nanoTime();
        return switch (detailImport.findDetail(isbn13)) {
            case NewBookImportOutcome outcome -> {
                if (outcome != NewBookImportOutcome.ALREADY_REGISTERED && outcome != NewBookImportOutcome.BUDGET_EXHAUSTED) {
                    logDetailElapsed(isbn13, startedAt);
                }
                yield outcome;
            }
            case NewBookDetailFound found -> registerBook(isbn13, found, startedAt);
        };
    }

    private NewBookImportOutcome registerBook(final String isbn13, final NewBookDetailFound found, final long startedAt) {
        try {
            if (registration.register(isbn13, found.detail())) {
                return NewBookImportOutcome.REGISTERED;
            }
            return NewBookImportOutcome.ALREADY_REGISTERED_AFTER_LOOKUP;
        } catch (final DuplicateImportedIsbnException exception) {
            return NewBookImportOutcome.ALREADY_REGISTERED_AFTER_LOOKUP;
        } catch (final DataIntegrityViolationException exception) {
            log.atWarn().addKeyValue("isbn13", isbn13).log("[BOOK] 신간 도서 저장 제약을 충족하지 못했습니다. isbn13={}", isbn13);
            return NewBookImportOutcome.STORAGE_REJECTED;
        } finally {
            logDetailElapsed(isbn13, startedAt);
        }
    }

    private void logDetailElapsed(final String isbn13, final long startedAt) {
        final long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
        log.atDebug().addKeyValue("isbn13", isbn13).addKeyValue("elapsedMs", elapsedMs)
            .log("[BOOK] 신간 도서 상세 처리가 종료됐습니다. isbn13={}, elapsedMs={}", isbn13, elapsedMs);
    }
}
