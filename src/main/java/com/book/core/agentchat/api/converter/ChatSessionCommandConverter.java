package com.book.core.agentchat.api.converter;

import com.book.core.agentchat.api.request.SendChatMessageRequest;
import com.book.core.agentchat.application.command.GetChatHistoryCommand;
import com.book.core.agentchat.application.command.GetChatSessionsCommand;
import com.book.core.agentchat.application.command.SendChatMessageCommand;
import org.springframework.stereotype.Component;

@Component
public class ChatSessionCommandConverter {
    public GetChatSessionsCommand toListCommand(final Long userId, final Long beforeId, final int limit) {
        return new GetChatSessionsCommand(userId, beforeId, limit);
    }

    public GetChatHistoryCommand toHistoryCommand(final Long userId, final Long sessionId, final long afterSequence, final int limit) {
        return new GetChatHistoryCommand(userId, sessionId, afterSequence, limit);
    }

    public SendChatMessageCommand toSendCommand(final Long userId, final Long sessionId, final SendChatMessageRequest request) {
        return new SendChatMessageCommand(userId, sessionId, request.idempotencyKey(), request.message(), request.consented(),
            request.excludeBookIds());
    }
}
