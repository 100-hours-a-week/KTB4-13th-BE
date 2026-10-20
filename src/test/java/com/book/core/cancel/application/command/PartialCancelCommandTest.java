package com.book.core.cancel.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cancel.domain.PartialCancelAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PartialCancelCommandTest {
    private final PartialCancelAction action = new PartialCancelAction("order", 10L, 1L);

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "bad key", "한글", "key/one", "key\n"})
    void 요청키가_잘못되면_Command_생성_즉시_거절한다(final String requestKey) {
        assertThatThrownBy(() -> new PartialCancelCommand(1L, action, requestKey)).isInstanceOfSatisfying(CoreException.class,
            error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void 요청키_길이는_100자까지_허용한다() {
        final String requestKey = "a".repeat(100);
        assertThat(new PartialCancelCommand(1L, action, requestKey).requestKey()).isEqualTo(requestKey);
        assertThatThrownBy(() -> new PartialCancelCommand(1L, action, requestKey + "a")).isInstanceOf(CoreException.class);
    }

    @Test
    void 허용문자를_포함한_요청키는_그대로_유지한다() {
        final String requestKey = "request_AZaz09-1";
        assertThat(new PartialCancelCommand(1L, action, requestKey).requestKey()).isEqualTo(requestKey);
    }
}
