package com.book.core.memory.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.util.Set;

public record ConversationMessage(String role, String text) {
    private static final Set<String> ROLES = Set.of("user", "assistant");
    private static final int MAX_TEXT_LENGTH = 4_000;

    public ConversationMessage {
        if (role == null || !ROLES.contains(role) || text == null || text.codePointCount(0, text.length()) > MAX_TEXT_LENGTH) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
