package com.book.core.agentchat.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.agentchat.application.port.ChatRepositoryPort;
import com.book.core.agentchat.application.result.MemoryExtractionTarget;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetMemoryExtractionTargetsUseCase {
    private final ChatRepositoryPort chats;
    private final Clock clock;

    /** 오늘 0시(서비스 시간대) 전까지의 대화 중 아직 반영하지 않은 세션을 고르며, 이전 밤에 실패하거나 빠진 세션도 포함합니다. */
    @Transactional(readOnly = true)
    public List<MemoryExtractionTarget> execute() {
        return chats.findMemoryExtractionTargets(LocalDate.now(clock).atStartOfDay(clock.getZone()).toInstant());
    }
}
