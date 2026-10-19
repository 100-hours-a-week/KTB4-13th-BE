package com.book.core.agentchat.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;

public record GetChatSessionsCommand(Long userId, Long beforeId, int limit) {
    public GetChatSessionsCommand {
        if (userId == null || userId <= 0 || (beforeId != null && beforeId <= 0) || limit < 1 || limit > 100) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
