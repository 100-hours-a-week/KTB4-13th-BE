package com.book.core.book.application.service;

import com.book.core.book.application.result.NewBookImportOutcome;
import java.util.EnumMap;
import java.util.UUID;

final class NewBookImportRun {
    final EnumMap<NewBookImportOutcome, Integer> outcomes = new EnumMap<>(NewBookImportOutcome.class);
    UUID runId;
    int candidateCount;
    int detailCallCount;
    long elapsedMs;
    String failureReason;
}
