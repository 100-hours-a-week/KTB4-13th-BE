package com.book.core.review.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ReviewContentTest {
    @ParameterizedTest
    @ValueSource(strings = {"0.5", "1.0", "4.500", "5.0"})
    void 반점_단위의_별점을_소수_한_자리로_정규화한다(final String rate) {
        final var content = new ReviewContent(new BigDecimal(rate), "x".repeat(255));

        assertThat(content.rate()).isEqualByComparingTo(rate);
        assertThat(content.rate().scale()).isEqualTo(1);
        assertThat(content.content()).hasSize(255);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.5", "0", "0.4", "5.5", "4.1", "4.05"})
    void 범위_밖이거나_반점_단위가_아닌_별점을_거부한다(final String rate) {
        assertInvalid(new BigDecimal(rate), "본문");
    }

    @Test
    void 필수값과_본문_규칙을_검증한다() {
        assertInvalid(null, "본문");
        assertInvalid(BigDecimal.ONE, null);
        assertInvalid(BigDecimal.ONE, "");
        assertInvalid(BigDecimal.ONE, " \t\n");
        assertInvalid(BigDecimal.ONE, "x".repeat(256));
    }

    private void assertInvalid(final BigDecimal rate, final String content) {
        assertThatThrownBy(() -> new ReviewContent(rate, content)).isInstanceOfSatisfying(CoreException.class,
            exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }
}
