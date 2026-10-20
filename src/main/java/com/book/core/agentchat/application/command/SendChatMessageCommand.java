package com.book.core.agentchat.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

public record SendChatMessageCommand(Long userId, Long sessionId, String requestKey, String message, boolean consented,
    List<Long> excludeBookIds) {
    public SendChatMessageCommand {
        if (userId == null || userId <= 0 || sessionId == null || sessionId <= 0 || requestKey == null || requestKey.isBlank()
            || requestKey.length() > 100 || message == null || message.isBlank() || message.length() > 200) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (excludeBookIds == null) {
            excludeBookIds = List.of();
        }
        if (excludeBookIds.stream().anyMatch((final Long id) -> id == null || id <= 0)) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        excludeBookIds = List.copyOf(excludeBookIds);
    }

    public String fingerprint() {
        final String input = message.length() + ":" + message + ":" + consented + ":" + excludeBookIds;
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (final NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
