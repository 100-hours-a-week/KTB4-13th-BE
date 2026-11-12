package com.book.core.book.application.port;

public interface BookDetailCallBudget {
    boolean tryAcquire();

    void pauseAfterRateLimit();
}
