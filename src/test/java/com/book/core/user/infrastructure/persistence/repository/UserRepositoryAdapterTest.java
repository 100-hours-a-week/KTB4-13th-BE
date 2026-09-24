package com.book.core.user.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.book.common.exception.BusinessException;
import com.book.core.user.domain.User;
import com.book.core.user.domain.exception.UserErrorCode;
import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;

class UserRepositoryAdapterTest {
    @Test
    void nickname_UNIQUE_위반이_아닌_기술_예외는_그대로_전파한다() {
        final UserJpaRepository jpaRepository = mock(UserJpaRepository.class);
        final var adapter = new UserRepositoryAdapter(jpaRepository);
        final var technicalException = new DataAccessResourceFailureException("unavailable");
        when(jpaRepository.saveAndFlush(any())).thenThrow(technicalException);

        assertThatThrownBy(() -> adapter.save(User.create("북적이"))).isSameAs(technicalException);
    }

    @Test
    void nickname_UNIQUE_위반을_nickname_충돌로_변환한다() {
        final UserJpaRepository jpaRepository = mock(UserJpaRepository.class);
        final var adapter = new UserRepositoryAdapter(jpaRepository);
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

    @Test
    void 조회에서_발생한_기술_예외는_그대로_전파한다() {
        final UserJpaRepository jpaRepository = mock(UserJpaRepository.class);
        final var adapter = new UserRepositoryAdapter(jpaRepository);
        final var technicalException = new DataAccessResourceFailureException("unavailable");
        when(jpaRepository.findById(1L)).thenThrow(technicalException);

        assertThatThrownBy(() -> adapter.findById(1L)).isSameAs(technicalException);
    }
}
