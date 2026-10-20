package com.book.core.agentchat.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;

public record GetChatHistoryCommand(Long userId, Long sessionId, long afterSequence, int limit) {
    public GetChatHistoryCommand {
        if (userId == null || userId <= 0 || sessionId == null || sessionId <= 0 || afterSequence < 0 || limit < 1 || limit > 100) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
