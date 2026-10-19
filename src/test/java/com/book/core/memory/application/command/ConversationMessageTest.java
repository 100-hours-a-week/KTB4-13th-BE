package com.book.core.memory.application.command;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ConversationMessageTest {
    @Test
    void 사용자와_챗봇의_4000자_이하_메시지를_허용한다() {
        assertThatCode(() -> new ConversationMessage("user", "가".repeat(4_000))).doesNotThrowAnyException();
        assertThatCode(() -> new ConversationMessage("assistant", "")).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"system", "USER"})
    void 사용자와_챗봇이_아닌_역할은_거부한다(final String role) {
        assertThatThrownBy(() -> new ConversationMessage(role, "안녕하세요")).isInstanceOf(CoreException.class);
    }

    @Test
    void 메시지가_없거나_4000자를_넘으면_거부한다() {
        assertThatThrownBy(() -> new ConversationMessage("user", null)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new ConversationMessage("user", "가".repeat(4_001))).isInstanceOf(CoreException.class);
    }
}
