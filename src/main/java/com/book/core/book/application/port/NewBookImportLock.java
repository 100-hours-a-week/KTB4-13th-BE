package com.book.core.book.application.port;

public interface NewBookImportLock {
    boolean runIfAcquired(Runnable batch);
}
