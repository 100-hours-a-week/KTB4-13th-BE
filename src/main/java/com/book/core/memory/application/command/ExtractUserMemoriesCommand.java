package com.book.core.memory.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.util.List;

public record ExtractUserMemoriesCommand(Long userId, String conversationId, List<ConversationMessage> messages) {
    public ExtractUserMemoriesCommand {
        if (userId == null || userId <= 0 || conversationId == null || conversationId.isBlank() || messages == null) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        messages = List.copyOf(messages);
    }
}
