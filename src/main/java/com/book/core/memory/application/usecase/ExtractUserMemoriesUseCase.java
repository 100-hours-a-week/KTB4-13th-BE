package com.book.core.memory.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.memory.application.command.ConversationMessage;
import com.book.core.memory.application.command.ExtractUserMemoriesCommand;
import com.book.core.memory.application.command.UserMemoryExtraction;
import com.book.core.memory.application.port.AiMemoryExtractionClient;
import com.book.core.memory.application.port.AiMemoryExtractionRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ExtractUserMemoriesUseCase {
    private static final int MAX_MESSAGES = 40;

    private final AiMemoryExtractionClient aiMemoryExtractionClient;

    public List<UserMemoryExtraction> execute(final ExtractUserMemoriesCommand command) {
        final List<ConversationMessage> messages = command.messages();
        final List<ConversationMessage> recentMessages = messages.subList(Math.max(0, messages.size() - MAX_MESSAGES), messages.size());
        return aiMemoryExtractionClient
            .extract(new AiMemoryExtractionRequest(command.userId(), true, command.conversationId(), recentMessages));
    }
}
