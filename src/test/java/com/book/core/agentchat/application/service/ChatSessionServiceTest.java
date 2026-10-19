package com.book.core.agentchat.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.common.exception.RetryAfterException;
import com.book.core.agentchat.application.command.SendChatMessageCommand;
import com.book.core.agentchat.application.result.ChatAttemptResult;
import com.book.core.agentchat.application.result.ChatResponseSnapshot;
import com.book.core.agentchat.application.usecase.BeginChatRequestUseCase;
import com.book.core.agentchat.application.usecase.CompleteChatRequestUseCase;
import com.book.core.agentchat.application.usecase.CreateChatSessionUseCase;
import com.book.core.agentchat.application.usecase.FailChatRequestUseCase;
import com.book.core.agentchat.application.usecase.GetChatHistoryUseCase;
import com.book.core.agentchat.application.usecase.GetChatSessionsUseCase;
import com.book.core.recommendation.application.result.ChatRecommendationAiOutcome;
import com.book.core.recommendation.application.usecase.ChatRecommendationUseCase;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

class ChatSessionServiceTest {
    private final BeginChatRequestUseCase begin = mock(BeginChatRequestUseCase.class);
    private final ChatRecommendationUseCase recommend = mock(ChatRecommendationUseCase.class);
    private final CompleteChatRequestUseCase complete = mock(CompleteChatRequestUseCase.class);
    private final FailChatRequestUseCase fail = mock(FailChatRequestUseCase.class);
    private final ChatSessionService service = new ChatSessionService(mock(CreateChatSessionUseCase.class),
        mock(GetChatHistoryUseCase.class), mock(GetChatSessionsUseCase.class), begin, recommend, complete, fail);
    private final SendChatMessageCommand command = new SendChatMessageCommand(1L, 2L, "key", "질문", true, List.of());

    @Test
    void 저장된_성공_응답은_AI를_호출하지_않고_반환한다() {
        final ChatResponseSnapshot snapshot = new ChatResponseSnapshot(1, Map.of(), "답변", List.of(), null, List.of(), false);
        when(begin.execute(command)).thenReturn(new ChatAttemptResult(3L, 1, null, snapshot, null, null));
        assertThat(service.send(command)).isSameAs(snapshot);
        verify(recommend, never()).execute(any());
    }

    @Test
    void 저장된_재시도_오류도_AI_호출_없이_재생한다() {
        when(begin.execute(command)).thenReturn(new ChatAttemptResult(3L, 1, null, null, ErrorCode.AI_RECOMMENDATION_RATE_LIMITED, 17L));
        assertThatThrownBy(() -> service.send(command)).isInstanceOfSatisfying(RetryAfterException.class,
            (final RetryAfterException exception) -> assertThat(exception.retryAfterSeconds()).isEqualTo(17));
        verify(recommend, never()).execute(any());
    }

    @Test
    void 저장된_일반_오류는_CoreException으로_재생한다() {
        when(begin.execute(command)).thenReturn(new ChatAttemptResult(3L, 1, null, null, ErrorCode.AI_RECOMMENDATION_TIMEOUT, null));
        assertThatThrownBy(() -> service.send(command)).isInstanceOfSatisfying(CoreException.class,
            (final CoreException exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.AI_RECOMMENDATION_TIMEOUT));
        verify(recommend, never()).execute(any());
    }

    @Test
    void 확정된_AI_오류를_기록하고_원래_예외를_호출자에게_전달한다() {
        when(begin.execute(command)).thenReturn(new ChatAttemptResult(3L, 1, null, null, null, null));
        final RetryAfterException exception = new RetryAfterException(ErrorCode.AI_RECOMMENDATION_UNAVAILABLE, 17);
        when(recommend.execute(null)).thenThrow(exception);
        assertThatThrownBy(() -> service.send(command)).isSameAs(exception);
        verify(fail).execute(command, 1, ErrorCode.AI_RECOMMENDATION_UNAVAILABLE, 17L);
        verify(complete, never()).execute(any(), ArgumentMatchers.anyLong(), any());
    }

    @Test
    void 완료_저장_예외를_AI_실패로_확정하지_않는다() {
        final ChatRecommendationAiOutcome outcome = new ChatRecommendationAiOutcome(Map.of(), "답변", List.of(), null, List.of(), false);
        when(begin.execute(command)).thenReturn(new ChatAttemptResult(3L, 1, null, null, null, null));
        when(recommend.execute(null)).thenReturn(outcome);
        final CoreException exception = new CoreException(ErrorCode.STORAGE_FAILURE);
        when(complete.execute(command, 1, outcome)).thenThrow(exception);
        assertThatThrownBy(() -> service.send(command)).isSameAs(exception);
        verify(fail, never()).execute(any(), ArgumentMatchers.anyLong(), any());
    }
}
