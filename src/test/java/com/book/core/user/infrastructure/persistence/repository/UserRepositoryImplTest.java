package com.book.core.user.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.book.common.exception.BusinessException;
import com.book.common.exception.CommonErrorCode;
import com.book.core.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

class UserRepositoryImplTest {
    @Test
    void 저장과_조회에서_발생한_기술_예외를_공통_계약으로_변환한다() {
        final UserJpaRepository jpaRepository = mock(UserJpaRepository.class);
        final var adapter = new UserRepositoryImpl(jpaRepository);
        when(jpaRepository.saveAndFlush(any())).thenThrow(new DataAccessResourceFailureException("unavailable"));
        when(jpaRepository.findById(1L)).thenThrow(new DataAccessResourceFailureException("unavailable"));
        assertThatThrownBy(() -> adapter.save(User.create("북적이")))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(CommonErrorCode.STORAGE_FAILURE));
        assertThatThrownBy(() -> adapter.findById(1L))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(CommonErrorCode.STORAGE_FAILURE));
    }
}
