package com.book.core.book.application.port;

public final class DuplicateImportedIsbnException extends RuntimeException {
    public DuplicateImportedIsbnException(final Throwable cause) {
        super("신간 도서 ISBN이 이미 등록됐습니다.", cause);
    }
}
