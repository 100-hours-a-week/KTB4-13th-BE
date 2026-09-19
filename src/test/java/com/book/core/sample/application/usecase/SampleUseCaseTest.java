package com.book.core.sample.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.sample.application.command.SampleCreateCommand;
import com.book.core.sample.application.command.SampleQueryCommand;
import com.book.core.sample.application.port.SampleRepository;
import com.book.core.sample.domain.Sample;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SampleUseCaseTest {
    @Mock
    SampleRepository repository;

    @Test
    void 정규화한_샘플을_저장하고_저장소가_할당한_ID를_반환한다() {
        when(repository.save(any(Sample.class))).thenReturn(Sample.restore(42L, "책"));
        final var result = new SampleCreateUseCase(repository).execute(new SampleCreateCommand("  책  "));
        final var saved = ArgumentCaptor.forClass(Sample.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().id()).isNull();
        assertThat(saved.getValue().name()).isEqualTo("책");
        assertThat(result.id()).isEqualTo(42L);
        assertThat(result.name()).isEqualTo("책");
    }

    @Test
    void 요청한_ID의_샘플을_조회한다() {
        when(repository.findById(42L)).thenReturn(Optional.of(Sample.restore(42L, "책")));
        final var result = new SampleQueryUseCase(repository).execute(new SampleQueryCommand(42L));
        assertThat(result.id()).isEqualTo(42L);
        assertThat(result.name()).isEqualTo("책");
        verify(repository).findById(42L);
    }

    @Test
    void 존재하지_않는_샘플은_공통_예외_계약으로_알린다() {
        when(repository.findById(42L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> new SampleQueryUseCase(repository).execute(new SampleQueryCommand(42L)))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(ErrorCode.SAMPLE_NOT_FOUND));
    }

    @Test
    void Command는_생성할_때_업무_전제조건을_검증한다() {
        assertThatThrownBy(() -> new SampleCreateCommand(" ")).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new SampleCreateCommand("가".repeat(101))).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new SampleQueryCommand(null)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new SampleQueryCommand(0L)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new SampleQueryCommand(-1L)).isInstanceOf(CoreException.class);
    }
}
