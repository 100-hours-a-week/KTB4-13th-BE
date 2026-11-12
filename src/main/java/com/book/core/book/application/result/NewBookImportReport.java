package com.book.core.book.application.result;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public record NewBookImportReport(UUID runId, long elapsedMs, int candidateCount, int detailCallCount,
    Map<NewBookImportOutcome, Integer> outcomes, String failureReason) {
    public NewBookImportReport {
        final var snapshot = new EnumMap<NewBookImportOutcome, Integer>(NewBookImportOutcome.class);
        snapshot.putAll(outcomes);
        outcomes = Collections.unmodifiableMap(snapshot);
    }

    public boolean failed() {
        return failureReason != null;
    }

    public String status() {
        if (failed()) {
            return "실패했습니다";
        }
        return "종료됐습니다";
    }

    public int registeredCount() {
        return outcomes.getOrDefault(NewBookImportOutcome.REGISTERED, 0);
    }
}
