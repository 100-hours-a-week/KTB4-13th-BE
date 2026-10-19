package com.book.core.memory.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.memory.application.port.UserMemoryRepositoryPort;
import com.book.core.memory.domain.UserMemory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class DeleteAllUserMemoriesUseCase {
    private final UserMemoryRepositoryPort userMemoryRepository;

    @Transactional
    public int execute(final Long userId) {
        userMemoryRepository.lockByUserId(userId);
        final List<UserMemory> activeMemories = userMemoryRepository.findActiveByUserId(userId);
        activeMemories.forEach(UserMemory::delete);
        return activeMemories.size();
    }
}
