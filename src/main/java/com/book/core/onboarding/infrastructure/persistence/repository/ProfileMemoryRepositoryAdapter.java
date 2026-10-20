package com.book.core.onboarding.infrastructure.persistence.repository;

import static com.book.core.memory.domain.QUserMemory.userMemory;

import com.book.common.config.time.TimeConfig;
import com.book.core.memory.domain.UserMemory;
import com.book.core.onboarding.application.port.AiPersonalizationProfileMemory;
import com.book.core.onboarding.application.port.ProfileMemoryRepositoryPort;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class ProfileMemoryRepositoryAdapter implements ProfileMemoryRepositoryPort {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<AiPersonalizationProfileMemory> findLatestActive(final Long userId, final int limit) {
        return queryFactory.selectFrom(userMemory).where(userMemory.userId.eq(userId), userMemory.deletedAt.isNull())
            .orderBy(userMemory.createdAt.desc(), userMemory.id.desc()).limit(limit).fetch().stream().map(this::toProfileMemory).toList();
    }

    // created_at은 JVM 기본 시간대로 저장되므로 명세의 한국 시간으로 바꿉니다.
    private AiPersonalizationProfileMemory toProfileMemory(final UserMemory memory) {
        return new AiPersonalizationProfileMemory(memory.type().name().toLowerCase(Locale.ROOT), memory.value(), memory.strength(),
            memory.categories(), memory.keyword(),
            memory.createdAt().atZone(ZoneId.systemDefault()).withZoneSameInstant(TimeConfig.SERVICE_ZONE).toLocalDateTime(),
            memory.vector(), memory.dim());
    }
}
