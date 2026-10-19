package com.book.core.memory.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.util.List;

public record DeleteUserMemoriesCommand(Long userId, List<Long> memoryIds) {
    public DeleteUserMemoriesCommand {
        if (userId == null || userId <= 0 || memoryIds == null || memoryIds.isEmpty()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (memoryIds.stream().anyMatch((final Long memoryId) -> memoryId == null || memoryId <= 0)) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        memoryIds = memoryIds.stream().distinct().toList();
    }
}
