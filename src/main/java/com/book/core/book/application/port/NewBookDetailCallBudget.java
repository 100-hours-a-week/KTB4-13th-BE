package com.book.core.book.application.port;

public interface NewBookDetailCallBudget {
    boolean tryAcquire();

    void pauseAfterRateLimit();
}
