package com.book.core.onboarding.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class GetOnboardingBookCandidatesCommandTest {
    @Test
    void 중복을_제거하고_요청_순서를_유지한다() {
        assertThat(new GetOnboardingBookCandidatesCommand(List.of("science-space", "novel-sf", "science-space")).subcategoryCodes())
            .containsExactly("science-space", "novel-sf");
    }

    @Test
    void null_빈_목록_빈_code_10개_선택은_거부한다() {
        assertThatThrownBy(() -> new GetOnboardingBookCandidatesCommand(null)).isInstanceOf(CoreException.class);
        for (final var codes : List.of(List.<String>of(), Arrays.asList("novel-sf", null), List.of(" "),
            IntStream.range(0, 10).mapToObj((final int index) -> "code-" + index).toList())) {
            assertThatThrownBy(() -> new GetOnboardingBookCandidatesCommand(codes)).isInstanceOf(CoreException.class);
        }
    }

    @Test
    void 최대_9개까지_허용하고_결과는_불변이다() {
        final var codes = IntStream.range(0, 9).mapToObj((final int index) -> "code-" + index).toList();
        final var command = new GetOnboardingBookCandidatesCommand(codes);
        assertThat(command.subcategoryCodes()).hasSize(9);
        assertThatThrownBy(() -> command.subcategoryCodes().add("code-9")).isInstanceOf(UnsupportedOperationException.class);
    }
}
