package com.book.core.memory.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.memory.application.command.ConversationMessage;
import com.book.core.memory.application.command.ExtractUserMemoriesCommand;
import com.book.core.memory.application.command.UserMemoryExtraction;
import com.book.core.memory.application.port.AiMemoryExtractionClient;
import com.book.core.memory.application.port.AiMemoryExtractionRequest;
import com.book.core.memory.domain.UserMemory;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ExtractUserMemoriesUseCase {
    public static final int MAX_MESSAGES = 40;

    private final AiMemoryExtractionClient aiMemoryExtractionClient;

    public List<UserMemoryExtraction> execute(final ExtractUserMemoriesCommand command) {
        final List<ConversationMessage> messages = command.messages();
        final List<ConversationMessage> recentMessages = messages.subList(Math.max(0, messages.size() - MAX_MESSAGES), messages.size());
        final List<UserMemoryExtraction> extractions = aiMemoryExtractionClient
            .extract(new AiMemoryExtractionRequest(command.userId(), true, command.conversationId(), recentMessages));
        extractions.forEach((final UserMemoryExtraction extraction) -> validate(command, extraction));
        return extractions;
    }

    // AI가 반환한 값이 기억 규칙을 어기면 요청 오류가 아닌 외부 응답 오류로 구분합니다.
    private void validate(final ExtractUserMemoriesCommand command, final UserMemoryExtraction extraction) {
        try {
            new UserMemory(command.userId(), extraction.type(), extraction.value(), extraction.strength(), extraction.categories(),
                extraction.keyword(), extraction.vector(), extraction.dim(), command.conversationId());
        } catch (final CoreException exception) {
            throw new CoreException(ErrorCode.AI_MEMORY_EXTRACTION_INVALID_RESPONSE, exception);
        }
    }
}
