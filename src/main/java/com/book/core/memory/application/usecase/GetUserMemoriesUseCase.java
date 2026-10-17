package com.book.core.memory.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.memory.application.port.UserMemoryRepositoryPort;
import com.book.core.memory.application.result.UserMemoryGroupResult;
import com.book.core.memory.domain.UserMemory;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetUserMemoriesUseCase {
    private static final Comparator<UserMemory> LATEST_FIRST =
        Comparator.comparing(UserMemory::createdAt).thenComparing(UserMemory::id).reversed();

    private final UserMemoryRepositoryPort userMemoryRepository;

    @Transactional(readOnly = true)
    public List<UserMemoryGroupResult> execute(final Long userId) {
        final Map<String, List<UserMemory>> groups = new LinkedHashMap<>();
        userMemoryRepository.findActiveByUserId(userId).stream().sorted(LATEST_FIRST).forEach((final UserMemory memory) -> groups
            .computeIfAbsent(memory.type() + ":" + memory.normalizedKeyword(), (final String key) -> new ArrayList<>()).add(memory));
        return groups.values().stream().map(this::toResult).toList();
    }

    private UserMemoryGroupResult toResult(final List<UserMemory> memories) {
        final UserMemory latest = memories.getFirst();
        final List<Long> memoryIds = memories.stream().map(UserMemory::id).toList();
        final List<String> categories = memories.stream().map(UserMemory::categories).flatMap(Collection::stream).distinct().toList();
        final List<LocalDate> conversationDates =
            memories.stream().map((final UserMemory memory) -> memory.createdAt().toLocalDate()).distinct().toList();
        return new UserMemoryGroupResult(memoryIds, latest.type(), latest.value(), categories, conversationDates);
    }
}
