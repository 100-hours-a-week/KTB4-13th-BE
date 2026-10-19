package com.book.core.memory.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.memory.application.command.DeleteUserMemoriesCommand;
import com.book.core.memory.application.port.UserMemoryRepositoryPort;
import com.book.core.memory.domain.UserMemory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class DeleteUserMemoriesUseCase {
    private final UserMemoryRepositoryPort userMemoryRepository;

    @Transactional
    public int execute(final DeleteUserMemoriesCommand command) {
        userMemoryRepository.lockByUserId(command.userId());
        final List<UserMemory> memories = userMemoryRepository.findAllByIdIn(command.memoryIds());
        if (memories.size() != command.memoryIds().size()
            || memories.stream().anyMatch((final UserMemory memory) -> !memory.isOwnedBy(command.userId()))) {
            throw new CoreException(ErrorCode.USER_MEMORY_NOT_FOUND);
        }
        final List<UserMemory> activeMemories = memories.stream().filter(UserMemory::isActive).toList();
        activeMemories.forEach(UserMemory::delete);
        return activeMemories.size();
    }
}
