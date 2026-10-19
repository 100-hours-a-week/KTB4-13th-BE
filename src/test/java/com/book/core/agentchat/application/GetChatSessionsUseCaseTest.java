package com.book.core.agentchat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.core.agentchat.application.command.GetChatSessionsCommand;
import com.book.core.agentchat.application.port.ChatRepositoryPort;
import com.book.core.agentchat.application.result.ChatSessionPageResult;
import com.book.core.agentchat.application.usecase.GetChatSessionsUseCase;
import com.book.core.agentchat.domain.ChatSession;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetChatSessionsUseCaseTest {
    private final ChatRepositoryPort chats = mock(ChatRepositoryPort.class);
    private final GetChatSessionsUseCase useCase = new GetChatSessionsUseCase(chats);

    @Test
    void 다음_페이지가_있을_때만_마지막_방을_커서로_제공한다() {
        final List<ChatSession> sessions = List.of(session(30L), session(20L), session(10L));
        when(chats.findSessions(7L, null, 3)).thenReturn(sessions);
        final ChatSessionPageResult first = useCase.execute(new GetChatSessionsCommand(7L, null, 2));
        assertThat(first.sessions()).extracting("sessionId").containsExactly(30L, 20L);
        assertThat(first.nextBeforeId()).isEqualTo(20L);
        final List<ChatSession> remaining = List.of(session(10L));
        when(chats.findSessions(7L, 20L, 3)).thenReturn(remaining);
        final ChatSessionPageResult last = useCase.execute(new GetChatSessionsCommand(7L, 20L, 2));
        assertThat(last.sessions()).extracting("sessionId").containsExactly(10L);
        assertThat(last.nextBeforeId()).isNull();
    }

    @Test
    void 빈_목록은_다음_커서가_없고_잘못된_범위는_거절한다() {
        when(chats.findSessions(7L, null, 21)).thenReturn(List.of());
        assertThat(useCase.execute(new GetChatSessionsCommand(7L, null, 20)).sessions()).isEmpty();
        assertThatThrownBy(() -> new GetChatSessionsCommand(7L, 0L, 20)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new GetChatSessionsCommand(7L, null, 101)).isInstanceOf(CoreException.class);
    }

    private ChatSession session(final long id) {
        final ChatSession session = mock(ChatSession.class);
        when(session.id()).thenReturn(id);
        return session;
    }
}
