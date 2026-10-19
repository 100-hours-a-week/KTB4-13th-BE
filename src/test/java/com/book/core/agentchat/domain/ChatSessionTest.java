package com.book.core.agentchat.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import org.junit.jupiter.api.Test;

class ChatSessionTest {
    @Test
    void 처리중인_방은_새_요청을_받지_않고_완료한_쌍에만_순서를_부여한다() {
        final ChatSession session = new ChatSession(7L);
        session.begin(11L);
        assertThatThrownBy(() -> session.begin(12L)).isInstanceOf(CoreException.class);
        session.release(11L);
        assertThat(session.nextSequence()).isEqualTo(1);
        session.begin(12L);
        assertThat(session.complete(12L)).isEqualTo(1);
        assertThat(session.nextSequence()).isEqualTo(3);
        assertThat(session.activeRequestId()).isNull();
    }
}
