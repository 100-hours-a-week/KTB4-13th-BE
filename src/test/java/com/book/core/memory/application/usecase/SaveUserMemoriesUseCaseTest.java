package com.book.core.memory.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.core.memory.application.command.SaveUserMemoriesCommand;
import com.book.core.memory.application.command.UserMemoryExtraction;
import com.book.core.memory.application.port.MemoryExtractionProgressPort;
import com.book.core.memory.application.port.UserMemoryRepositoryPort;
import com.book.core.memory.domain.UserMemoryType;
import java.util.List;
import org.junit.jupiter.api.Test;

class SaveUserMemoriesUseCaseTest {
    private final UserMemoryRepositoryPort repository = mock(UserMemoryRepositoryPort.class);
    private final MemoryExtractionProgressPort progress = mock(MemoryExtractionProgressPort.class);
    private final SaveUserMemoriesUseCase useCase = new SaveUserMemoriesUseCase(repository, progress);

    @Test
    void 이미_처리한_추출은_저장하지_않는다() {
        final SaveUserMemoriesCommand command = new SaveUserMemoriesCommand(1L, 2L, 3L, List.of());
        when(progress.markExtracted(1L, 2L, 3L)).thenReturn(false);

        assertThat(useCase.execute(command)).isFalse();
        verify(repository).lockByUserId(1L);
        verify(repository, never()).deleteActiveByUserIdAndConversationId(1L, "2");
        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void 새_추출은_기존_대화_기억을_교체하여_저장한다() {
        final UserMemoryExtraction extraction =
            new UserMemoryExtraction(UserMemoryType.TOPIC, "추리 소설을 좋아해요", 2, List.of(), "추리", List.of(0.1f), 1);
        final SaveUserMemoriesCommand command = new SaveUserMemoriesCommand(1L, 2L, 3L, List.of(extraction));
        when(progress.markExtracted(1L, 2L, 3L)).thenReturn(true);
        when(repository.findDeletedByUserId(1L)).thenReturn(List.of());

        assertThat(useCase.execute(command)).isTrue();
        verify(repository).lockByUserId(1L);
        verify(repository).deleteActiveByUserIdAndConversationId(1L, "2");
        verify(repository).saveAll(org.mockito.ArgumentMatchers.argThat(memories -> memories.size() == 1));
    }
}
