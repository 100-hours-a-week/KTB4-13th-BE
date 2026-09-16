package com.book.common.exception;

public enum CommonErrorCode implements ErrorCode {
    STORAGE_FAILURE;

    @Override
    public String code() { return name(); }

    @Override
    public String message() { return "저장소 작업을 완료할 수 없습니다."; }

    @Override
    public Category category() { return Category.INTERNAL_ERROR; }
}
