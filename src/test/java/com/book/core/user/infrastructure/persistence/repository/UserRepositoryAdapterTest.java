package com.book.core.user.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.book.core.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

class UserRepositoryAdapterTest {
    @Test
    void 저장에서_발생한_기술_예외는_그대로_전파한다() {
        final UserJpaRepository jpaRepository = mock(UserJpaRepository.class);
        final var adapter = new UserRepositoryAdapter(jpaRepository);
        final var technicalException = new DataAccessResourceFailureException("unavailable");
        when(jpaRepository.saveAndFlush(any())).thenThrow(technicalException);

        assertThatThrownBy(() -> adapter.save(User.create("북적이"))).isSameAs(technicalException);
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
