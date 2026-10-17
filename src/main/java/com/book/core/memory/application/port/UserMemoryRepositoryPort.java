package com.book.core.memory.application.port;

import com.book.core.memory.domain.UserMemory;
import java.util.List;

public interface UserMemoryRepositoryPort {
    List<UserMemory> findDeletedByUserIdAndConversationId(final Long userId, final String conversationId);

    void deleteActiveByUserIdAndConversationId(final Long userId, final String conversationId);

    List<UserMemory> saveAll(final List<UserMemory> memories);
}
