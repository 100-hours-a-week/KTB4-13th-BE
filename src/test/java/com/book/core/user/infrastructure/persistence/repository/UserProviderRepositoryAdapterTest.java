package com.book.core.user.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.book.common.exception.BusinessException;
import com.book.common.exception.ErrorCode;
import com.book.core.user.domain.ProviderType;
import com.book.core.user.domain.UserProvider;
import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;

class UserProviderRepositoryAdapterTest {
    @Test
    void provider_identity_UNIQUE_위반이_아닌_기술_예외는_그대로_전파한다() {
        final UserProviderJpaRepository jpaRepository = mock(UserProviderJpaRepository.class);
        final var adapter = new UserProviderRepositoryAdapter(jpaRepository);
        final var technicalException = new DataAccessResourceFailureException("unavailable");
        when(jpaRepository.saveAndFlush(any())).thenThrow(technicalException);

        assertThatThrownBy(() -> adapter.save(UserProvider.create(42L, ProviderType.KAKAO, "123", null)))
                .isSameAs(technicalException);
    }

    @Test
    void provider_identity_UNIQUE_위반을_provider_충돌로_변환한다() {
        final UserProviderJpaRepository jpaRepository = mock(UserProviderJpaRepository.class);
        final var adapter = new UserProviderRepositoryAdapter(jpaRepository);
        final var hibernateException = new ConstraintViolationException(
                "duplicate", new SQLException("duplicate", "23000", 1062), "uk_user_providers_identity_active_flag");
        when(jpaRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate", hibernateException));

        assertThatThrownBy(() -> adapter.save(UserProvider.create(42L, ProviderType.KAKAO, "123", null)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(ErrorCode.PROVIDER_IDENTITY_CONFLICT));
    }

    @Test
    void 활성_조회에서_발생한_기술_예외는_그대로_전파한다() {
        final UserProviderJpaRepository jpaRepository = mock(UserProviderJpaRepository.class);
        final var adapter = new UserProviderRepositoryAdapter(jpaRepository);
        final var technicalException = new DataAccessResourceFailureException("unavailable");
        when(jpaRepository.findByProviderTypeAndProviderUserIdAndDeletedAtIsNull(ProviderType.KAKAO, "123"))
                .thenThrow(technicalException);

        assertThatThrownBy(() -> adapter.findActiveByProviderTypeAndProviderUserId(ProviderType.KAKAO, "123"))
                .isSameAs(technicalException);
    }
}
