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

    @Transactional(readOnly = true)
    public List<MemoryExtractionTarget> execute() {
        return chats.findMemoryExtractionTargets(LocalDate.now(clock).atStartOfDay(clock.getZone()).toInstant());
    }
}
