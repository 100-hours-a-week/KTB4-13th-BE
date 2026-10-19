package com.book.core.memory.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.util.List;

public record SaveUserMemoriesCommand(Long userId, String conversationId, List<UserMemoryExtraction> extractions) {
    public SaveUserMemoriesCommand {
        if (userId == null || userId <= 0 || conversationId == null || conversationId.isBlank() || extractions == null) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        extractions = List.copyOf(extractions);
    }
}
