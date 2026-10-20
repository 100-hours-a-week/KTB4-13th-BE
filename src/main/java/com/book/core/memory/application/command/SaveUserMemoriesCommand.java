package com.book.core.memory.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.util.List;

public record SaveUserMemoriesCommand(Long userId, Long sessionId, long sequence, List<UserMemoryExtraction> extractions) {
    public SaveUserMemoriesCommand {
        if (userId == null || userId <= 0 || sessionId == null || sessionId <= 0 || sequence <= 0 || extractions == null) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        extractions = List.copyOf(extractions);
    }

    public String conversationId() {
        return String.valueOf(sessionId);
    }
}
