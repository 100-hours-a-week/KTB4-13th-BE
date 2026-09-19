package com.book.core.user.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.book.common.exception.BusinessException;
import com.book.common.exception.CommonErrorCode;
import com.book.core.user.domain.User;
import com.book.core.user.domain.exception.UserErrorCode;
import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;

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

    @Test
    void nickname_UNIQUE_위반을_nickname_충돌로_변환한다() {
        final UserJpaRepository jpaRepository = mock(UserJpaRepository.class);
        final var adapter = new UserRepositoryImpl(jpaRepository);
        final var hibernateException = new ConstraintViolationException(
                "duplicate", new SQLException("duplicate", "23000", 1062), "uk_users_nickname_active_flag");
        when(jpaRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate", hibernateException));

        assertThatThrownBy(() -> adapter.save(User.create("북적이")))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(UserErrorCode.NICKNAME_CONFLICT));
    }
}
