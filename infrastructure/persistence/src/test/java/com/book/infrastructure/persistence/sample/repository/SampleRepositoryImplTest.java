package com.book.infrastructure.persistence.sample.repository;

import com.book.common.exception.BusinessException;
import com.book.common.exception.CommonErrorCode;
import com.book.core.domain.sample.model.Sample;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SampleRepositoryImplTest {
    @Test
    void 저장과_조회에서_발생한_기술_예외를_공통_계약으로_변환한다() {
        SampleJpaRepository jpaRepository = mock(SampleJpaRepository.class);
        var adapter = new SampleRepositoryImpl(jpaRepository);
        when(jpaRepository.saveAndFlush(any())).thenThrow(new DataAccessResourceFailureException("unavailable"));
        when(jpaRepository.findById(1L)).thenThrow(new DataAccessResourceFailureException("unavailable"));
        assertThatThrownBy(() -> adapter.save(Sample.create("책")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(CommonErrorCode.STORAGE_FAILURE));
        assertThatThrownBy(() -> adapter.findById(1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(CommonErrorCode.STORAGE_FAILURE));
    }
}
