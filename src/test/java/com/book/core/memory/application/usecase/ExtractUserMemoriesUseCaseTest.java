package com.book.core.memory.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.memory.application.command.ConversationMessage;
import com.book.core.memory.application.command.ExtractUserMemoriesCommand;
import com.book.core.memory.application.port.AiMemoryExtractionRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ExtractUserMemoriesUseCaseTest {
    @Test
    void 동의한_사용자의_최근_40개_메시지만_AI에_보낸다() {
        final List<AiMemoryExtractionRequest> requests = new ArrayList<>();
        final ExtractUserMemoriesUseCase useCase = new ExtractUserMemoriesUseCase((final AiMemoryExtractionRequest request) -> {
            requests.add(request);
            return List.of();
        });
        final List<ConversationMessage> messages =
            IntStream.rangeClosed(1, 41).mapToObj((final int index) -> new ConversationMessage("user", "메시지" + index)).toList();

        useCase.execute(new ExtractUserMemoriesCommand(42L, "cv_1", messages));

        final AiMemoryExtractionRequest request = requests.getFirst();
        assertThat(request.userId()).isEqualTo(42L);
        assertThat(request.consented()).isTrue();
        assertThat(request.conversationId()).isEqualTo("cv_1");
        assertThat(request.conversation()).hasSize(40).first().isEqualTo(new ConversationMessage("user", "메시지2"));
    }
}
