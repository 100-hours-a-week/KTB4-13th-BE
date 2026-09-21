package com.book.core.sample.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.book.core.sample.domain.Sample;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

class SampleRepositoryImplTest {
    @Test
    void 저장과_조회에서_발생한_기술_예외를_그대로_전파한다() {
        final SampleJpaRepository jpaRepository = mock(SampleJpaRepository.class);
        final var adapter = new SampleRepositoryImpl(jpaRepository);
        when(jpaRepository.saveAndFlush(any())).thenThrow(new DataAccessResourceFailureException("unavailable"));
        when(jpaRepository.findById(1L)).thenThrow(new DataAccessResourceFailureException("unavailable"));
        assertThatThrownBy(() -> adapter.save(Sample.create("책")))
                .isInstanceOf(DataAccessResourceFailureException.class);
        assertThatThrownBy(() -> adapter.findById(1L)).isInstanceOf(DataAccessResourceFailureException.class);
    }
}
