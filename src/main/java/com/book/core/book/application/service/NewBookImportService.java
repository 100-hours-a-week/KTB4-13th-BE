package com.book.core.book.application.service;

import com.book.core.book.application.port.BookCatalogException;
import com.book.core.book.application.port.DuplicateImportedIsbnException;
import com.book.core.book.application.port.NewBookImportLock;
import com.book.core.book.application.port.NewBookImportNotifier;
import com.book.core.book.application.result.NewBookDetailFound;
import com.book.core.book.application.result.NewBookImportOutcome;
import com.book.core.book.application.result.NewBookImportReport;
import com.book.core.book.application.usecase.CollectNewBookIsbnsUseCase;
import com.book.core.book.application.usecase.ImportNewBookDetailUseCase;
import com.book.core.book.application.usecase.RegisterNewBookUseCase;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
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

    private final NewBookImportNotifier notifier;

    public void importNewBooks(final LocalDate today) {
        final var run = new NewBookImportRun();
        final String previousRunId = MDC.get("runId");
        try {
            if (!batchLock.runIfAcquired(() -> importCandidates(today, run))) {
                log.info("[BOOK] 다른 인스턴스가 신간 도서 등록 배치를 실행 중이어서 건너뜁니다.");
            }
        } catch (final RuntimeException exception) {
            if (exception instanceof BookCatalogException catalogException) {
                run.failureReason = catalogException.reason().description();
            } else {
                run.failureReason = exception.getClass().getSimpleName();
            }
            throw exception;
        } finally {
            try {
                if (run.runId != null) {
                    report(run);
                }
            } finally {
                if (previousRunId == null) {
                    MDC.remove("runId");
                } else {
                    MDC.put("runId", previousRunId);
                }
            }
        }
    }

    private void importCandidates(final LocalDate today, final NewBookImportRun run) {
        final long startedAt = System.nanoTime();
        run.runId = UUID.randomUUID();
        MDC.put("runId", run.runId.toString());
        try {
            log.info("[BOOK] 신간 도서 등록 배치를 시작합니다. runId={}", run.runId);
            final var candidateIsbns = collection.collect(today);
            run.candidateCount = candidateIsbns.size();
            for (final String isbn13 : candidateIsbns) {
                final NewBookImportOutcome outcome = importBook(isbn13, run);
                run.outcomes.merge(outcome, 1, Integer::sum);
                if (outcome == NewBookImportOutcome.BUDGET_EXHAUSTED || outcome == NewBookImportOutcome.RATE_LIMITED) {
                    break;
                }
            }
        } finally {
            run.elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
        }
    }

    private void report(final NewBookImportRun run) {
        final var report =
            new NewBookImportReport(run.runId, run.elapsedMs, run.candidateCount, run.detailCallCount, run.outcomes, run.failureReason);
        var event = log.atInfo();
        if (report.failed()) {
            event = log.atError().addKeyValue("reason", report.failureReason());
        }
        event.addKeyValue("candidateCount", report.candidateCount()).addKeyValue("detailCallCount", report.detailCallCount())
            .addKeyValue("outcomes", report.outcomes()).addKeyValue("elapsedMs", report.elapsedMs())
            .log("[BOOK] 신간 도서 등록 배치가 {}. runId={}, candidateCount={}, detailCallCount={}, outcomes={}, elapsedMs={}, reason={}",
                report.status(), report.runId(), report.candidateCount(), report.detailCallCount(), report.outcomes(), report.elapsedMs(),
                report.failureReason());
        try {
            notifier.send(report);
        } catch (final RuntimeException exception) {
            log.atWarn().addKeyValue("reason", exception.getClass().getSimpleName()).log("[BOOK] 신간 도서 등록 배치 디스코드 알림 전송에 실패했습니다. runId={}",
                report.runId());
        }
    }

    private NewBookImportOutcome importBook(final String isbn13, final NewBookImportRun run) {
        final long startedAt = System.nanoTime();
        return switch (detailImport.findDetail(isbn13)) {
            case NewBookImportOutcome outcome -> {
                if (outcome != NewBookImportOutcome.ALREADY_REGISTERED && outcome != NewBookImportOutcome.BUDGET_EXHAUSTED) {
                    run.detailCallCount++;
                    logDetailElapsed(isbn13, startedAt);
                }
                yield outcome;
            }
            case NewBookDetailFound found -> {
                run.detailCallCount++;
                yield registerBook(isbn13, found, startedAt);
            }
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
