package com.book.core.agentchat.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.book.core.agentchat.application.port.ChatRepositoryPort;
import com.book.core.agentchat.application.result.MemoryExtractionTarget;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetMemoryExtractionTargetsUseCaseTest {
    private final ChatRepositoryPort chats = mock(ChatRepositoryPort.class);

    @Test
    void 서비스_시간대의_오늘_0시_전까지_저장된_대화를_대상으로_조회한다() {
        final Clock clock = Clock.fixed(Instant.parse("2026-10-20T17:00:00Z"), ZoneId.of("Asia/Seoul"));
        final List<MemoryExtractionTarget> targets = List.of(new MemoryExtractionTarget(7L, 1L, 4L));
        when(chats.findMemoryExtractionTargets(Instant.parse("2026-10-20T15:00:00Z"))).thenReturn(targets);

        assertThat(new GetMemoryExtractionTargetsUseCase(chats, clock).execute()).isEqualTo(targets);
    }
}
