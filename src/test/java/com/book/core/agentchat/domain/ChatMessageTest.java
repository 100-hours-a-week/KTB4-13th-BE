package com.book.core.agentchat.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import org.junit.jupiter.api.Test;

class ChatMessageTest {
    @Test
    void 사용자_발화와_완성된_서비스_응답을_구분한다() {
        assertThat(new ChatMessage(1L, 2L, 1, "user", "질문", null).responsePayload()).isNull();
        assertThat(new ChatMessage(1L, 2L, 2, "assistant", "답변", "{}").role()).isEqualTo("assistant");
        assertThatThrownBy(() -> new ChatMessage(1L, 2L, 2, "assistant", "답변", null)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new ChatMessage(1L, 2L, 1, "user", "질문", "{}")).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new ChatMessage(1L, 2L, 1, "system", "지시", null)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new ChatMessage(1L, 2L, 0, "user", "질문", null)).isInstanceOf(CoreException.class);
    }
}
