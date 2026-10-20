package com.book.core.memory.application.port;

import com.book.core.memory.domain.UserMemory;
import java.util.List;

public interface UserMemoryRepositoryPort {
    /** 같은 회원의 기억 저장과 삭제를 직렬화하도록 회원 행을 트랜잭션 종료까지 잠급니다. */
    void lockByUserId(final Long userId);

    List<UserMemory> findActiveByUserId(final Long userId);

    List<UserMemory> findAllByIdIn(final List<Long> ids);

    List<UserMemory> findDeletedByUserId(final Long userId);

    void deleteActiveByUserIdAndConversationId(final Long userId, final String conversationId);

    List<UserMemory> saveAll(final List<UserMemory> memories);
}
