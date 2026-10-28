package com.book.core.point.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PointAmountTest {
    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "1", "1.00", "-1", "-1.00", "1E+3"})
    void 값이_정수이면_표현된_소수점이나_음수는_기존_규칙을_유지한다(final String value) {
        assertThatCode(() -> PointAmount.requireWhole(new BigDecimal(value))).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.1", "-0.1", "1.25", "-1.25"})
    void 소수_값은_거절한다(final String value) {
        assertThatThrownBy(() -> PointAmount.requireWhole(new BigDecimal(value))).isInstanceOfSatisfying(CoreException.class,
            (final CoreException exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }
}
