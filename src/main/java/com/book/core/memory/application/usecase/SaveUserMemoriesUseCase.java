package com.book.core.memory.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.memory.application.command.SaveUserMemoriesCommand;
import com.book.core.memory.application.command.UserMemoryExtraction;
import com.book.core.memory.application.port.UserMemoryRepositoryPort;
import com.book.core.memory.domain.UserMemory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class SaveUserMemoriesUseCase {
    private final UserMemoryRepositoryPort userMemoryRepository;

    @Transactional
    public void execute(final SaveUserMemoriesCommand command) {
        userMemoryRepository.lockByUserId(command.userId());
        final List<UserMemory> extracted =
            command.extractions().stream().map((final UserMemoryExtraction extraction) -> toMemory(command, extraction)).toList();
        final List<UserMemory> deleted =
            userMemoryRepository.findDeletedByUserIdAndConversationId(command.userId(), command.conversationId());
        final List<UserMemory> memories =
            extracted.stream().filter((final UserMemory memory) -> deleted.stream().noneMatch(memory::hasSameKeywordAs)).toList();

        userMemoryRepository.deleteActiveByUserIdAndConversationId(command.userId(), command.conversationId());
        userMemoryRepository.saveAll(memories);
    }

    private UserMemory toMemory(final SaveUserMemoriesCommand command, final UserMemoryExtraction extraction) {
        return new UserMemory(command.userId(), extraction.type(), extraction.value(), extraction.strength(), extraction.categories(),
            extraction.keyword(), extraction.vector(), extraction.dim(), command.conversationId());
    }
}
