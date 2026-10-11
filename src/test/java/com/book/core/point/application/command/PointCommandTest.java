package com.book.core.point.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.point.domain.enums.PointType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class PointCommandTest {
    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1})
    void 유효하지_않은_회원_ID는_조회_Command를_생성하지_않는다(final Long userId) {
        assertThatThrownBy(() -> new GetPointCommand(userId)).isInstanceOfSatisfying(CoreException.class,
            (final var exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void 필수값이_없는_쓰기_Command를_생성하지_않는다() {
        assertThatThrownBy(() -> new EarnPointCommand(42L, PointType.REVIEW, 10L, null)).isInstanceOf(CoreException.class);
    }
}
