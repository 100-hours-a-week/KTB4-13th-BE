package com.book.core.memory.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;

import com.book.core.memory.application.command.ConversationMessage;
import com.book.core.memory.application.command.ExtractUserMemoriesCommand;
import com.book.core.memory.application.command.UserMemoryExtraction;
import com.book.core.memory.application.port.AiMemoryExtractionRequest;
import com.book.core.memory.domain.UserMemoryType;
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

    @Test
    void AI가_기억_규칙을_어긴_세기나_벡터를_반환하면_잘못된_AI_응답으로_구분한다() {
        assertInvalidResponse(new UserMemoryExtraction(UserMemoryType.TOPIC, "추리 소설을 좋아함", 0, List.of(), "추리 소설", List.of(0.1f, 0.2f), 2));
        assertInvalidResponse(new UserMemoryExtraction(UserMemoryType.TOPIC, "추리 소설을 좋아함", 2, List.of(), "추리 소설", List.of(0.1f), 2));
    }

    private void assertInvalidResponse(final UserMemoryExtraction extraction) {
        final ExtractUserMemoriesUseCase useCase =
            new ExtractUserMemoriesUseCase((final AiMemoryExtractionRequest request) -> List.of(extraction));

        assertThatThrownBy(
            () -> useCase.execute(new ExtractUserMemoriesCommand(42L, "cv_1", List.of(new ConversationMessage("user", "메시지")))))
            .isInstanceOfSatisfying(CoreException.class, (final CoreException exception) -> assertThat(exception.errorCode())
                .isEqualTo(ErrorCode.AI_MEMORY_EXTRACTION_INVALID_RESPONSE));
    }
}
