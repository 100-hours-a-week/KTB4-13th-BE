package com.book.core.book.application.result;

public enum NewBookImportOutcome implements NewBookDetailLookup {
    ALREADY_REGISTERED,
    BUDGET_EXHAUSTED,
    MISSING_DETAIL,
    REGISTERED,
    ALREADY_REGISTERED_AFTER_LOOKUP,
    STORAGE_REJECTED,
    RATE_LIMITED,
    LOOKUP_FAILED
}
