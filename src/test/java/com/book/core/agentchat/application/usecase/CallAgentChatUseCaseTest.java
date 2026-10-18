package com.book.core.agentchat.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.agentchat.application.port.AgentChatClient;
import com.book.core.agentchat.application.result.AgentAction;
import com.book.core.agentchat.application.result.AgentChatContext;
import com.book.core.agentchat.application.result.AgentChosenBook;
import com.book.core.agentchat.application.result.AgentDecision;
import com.book.core.agentchat.application.result.AgentObservedProduct;
import com.book.core.agentchat.application.result.AgentSelection;
import com.book.core.recommendation.application.command.ChatRecommendationCommand;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CallAgentChatUseCaseTest {
    private static final String ISBN = "9788900000011";
    private final AgentChatClient client = mock(AgentChatClient.class);
    private final CallAgentChatUseCase useCase = new CallAgentChatUseCase(client);
    private final ChatRecommendationCommand command = new ChatRecommendationCommand(7L, true, Map.of(), "골라줘", List.of(), List.of());
    private final AgentChatContext context =
        new AgentChatContext(List.of(new AgentObservedProduct(11L, ISBN, "책", new BigDecimal("12000"), 0, 3, true)), List.of());

    @Test
    void 허용된_선택은_가격과_예산_개수를_검증한_후_그대로_반환한다() {
        final AgentDecision decision = selection("12000", "20000", 1);
        when(client.enabled()).thenReturn(true);
        when(client.chat(command, 8L, 9L, context)).thenReturn(decision);
        assertThat(useCase.enabled()).isTrue();
        assertThat(useCase.execute(command, 8L, 9L, context)).isEqualTo(decision);
        verify(client).chat(command, 8L, 9L, context);
    }

    @Test
    void AI가_관측_가격을_바꾸거나_예산과_개수를_어기면_거절한다() {
        assertInvalid(selection("13000", "20000", 1), context);
        assertInvalid(selection("12000", "10000", 1), context);
        assertInvalid(selection("12000", "20000", 2), context);
        assertInvalid(selection("12000", "20000", 1), new AgentChatContext(List.of(), List.of()));
    }

    @Test
    void 관측하지_않은_직접_변경은_거절하지만_정상_안내는_통과한다() {
        assertInvalid(new AgentDecision("shop", "담았어요", null, List.of(new AgentAction("set", "9788900000028", 2)), null, List.of()),
            context);
        final AgentDecision notice = new AgentDecision("shop", "이미 3권 담겨 있어요", null, List.of(), null, List.of());
        when(client.chat(command, 8L, 9L, context)).thenReturn(notice);
        assertThat(useCase.execute(command, 8L, 9L, context)).isEqualTo(notice);
    }

    private AgentDecision selection(final String price, final String budget, final int count) {
        return new AgentDecision("shop", "이 책을 담을까요?", null, List.of(),
            new AgentSelection(List.of(new AgentChosenBook(ISBN, new BigDecimal(price))), new BigDecimal(budget), count), List.of());
    }

    private void assertInvalid(final AgentDecision decision, final AgentChatContext observation) {
        when(client.chat(any(), anyLong(), anyLong(), any())).thenReturn(decision);
        assertThatThrownBy(() -> useCase.execute(command, 8L, 9L, observation)).isInstanceOfSatisfying(CoreException.class,
            (final CoreException error) -> assertThat(error.errorCode()).isEqualTo(ErrorCode.AI_SERVICE_FAILURE));
    }
}
