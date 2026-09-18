package com.book.core.sample.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SampleTest {
    @Test
    void 이름의_앞뒤_공백을_제거하여_생성한다() {
        final Sample sample = Sample.create("  책  ");
        assertThat(sample.id()).isNull();
        assertThat(sample.name()).isEqualTo("책");
    }

    @Test
    void 공백_제거_후_100자까지_허용한다() {
        assertThat(Sample.create("  " + "가".repeat(100) + "  ").name()).hasSize(100);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n", "\u2003"})
    void 비어_있는_이름을_거부한다(final String name) {
        assertThatThrownBy(() -> Sample.create(name))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_SAMPLE_NAME));
    }

    @Test
    void 이름이_101자이면_거부한다() {
        assertThatThrownBy(() -> Sample.create("가".repeat(101))).isInstanceOf(CoreException.class);
    }

    @Test
    void 저장된_샘플을_복원한다() {
        final Sample sample = Sample.restore(42L, "책");
        assertThat(sample.id()).isEqualTo(42L);
        assertThat(sample.name()).isEqualTo("책");
    }

    @Test
    void 복원할_때도_불변식을_검증한다() {
        assertThatThrownBy(() -> Sample.restore(null, "책")).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> Sample.restore(0L, "책")).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> Sample.restore(1L, " ")).isInstanceOf(CoreException.class);
    }
}
