package com.book.core.sample.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorType;
import com.book.core.sample.domain.Sample;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

class SampleRepositoryImplTest {
    @Test
    void 저장과_조회에서_발생한_기술_예외를_공통_계약으로_변환한다() {
        final SampleJpaRepository jpaRepository = mock(SampleJpaRepository.class);
        final var adapter = new SampleRepositoryImpl(jpaRepository);
        when(jpaRepository.saveAndFlush(any())).thenThrow(new DataAccessResourceFailureException("unavailable"));
        when(jpaRepository.findById(1L)).thenThrow(new DataAccessResourceFailureException("unavailable"));
        assertThatThrownBy(() -> adapter.save(Sample.create("책")))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        (final var exception) ->
                                assertThat(exception.errorType()).isEqualTo(ErrorType.STORAGE_FAILURE));
        assertThatThrownBy(() -> adapter.findById(1L))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        (final var exception) ->
                                assertThat(exception.errorType()).isEqualTo(ErrorType.STORAGE_FAILURE));
    }
}
