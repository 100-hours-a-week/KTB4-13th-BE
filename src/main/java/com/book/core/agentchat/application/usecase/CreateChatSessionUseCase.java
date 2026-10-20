package com.book.core.agentchat.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.agentchat.application.port.ChatRepositoryPort;
import com.book.core.agentchat.domain.ChatSession;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CreateChatSessionUseCase {
    private final ChatRepositoryPort chats;

    @Transactional
    public Long execute(final Long userId) {
        return chats.saveSession(new ChatSession(userId)).id();
    }
}
