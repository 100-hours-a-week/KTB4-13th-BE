package com.book.core.library.application.command;

public record CreateShelfPhotoUploadCommand(Long userId, String contentType, long contentLength) {
}
