package com.book.core.preference.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;

public record ExtractChatMemoriesCommand(Long userId, Long sessionId, long sequence) {
    public ExtractChatMemoriesCommand {
        if (userId == null || userId <= 0 || sessionId == null || sessionId <= 0 || sequence <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
