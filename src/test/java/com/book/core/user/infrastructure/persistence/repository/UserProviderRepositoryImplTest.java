package com.book.core.user.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.book.common.exception.BusinessException;
import com.book.common.exception.CommonErrorCode;
import com.book.core.user.domain.ProviderType;
import com.book.core.user.domain.UserProvider;
import com.book.core.user.domain.exception.UserErrorCode;
import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;

class UserProviderRepositoryImplTest {
    @Test
    void 저장과_활성_조회에서_발생한_기술_예외를_공통_계약으로_변환한다() {
        final UserProviderJpaRepository jpaRepository = mock(UserProviderJpaRepository.class);
        final var adapter = new UserProviderRepositoryImpl(jpaRepository);
        when(jpaRepository.saveAndFlush(any())).thenThrow(new DataAccessResourceFailureException("unavailable"));
        when(jpaRepository.findByProviderTypeAndProviderUserIdAndDeletedAtIsNull(ProviderType.KAKAO, "123"))
                .thenThrow(new DataAccessResourceFailureException("unavailable"));
        assertThatThrownBy(() -> adapter.save(UserProvider.create(42L, ProviderType.KAKAO, "123", null)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(CommonErrorCode.STORAGE_FAILURE));
        assertThatThrownBy(() -> adapter.findActiveByProviderTypeAndProviderUserId(ProviderType.KAKAO, "123"))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(CommonErrorCode.STORAGE_FAILURE));
    }

    @Test
    void provider_identity_UNIQUE_위반을_provider_충돌로_변환한다() {
        final UserProviderJpaRepository jpaRepository = mock(UserProviderJpaRepository.class);
        final var adapter = new UserProviderRepositoryImpl(jpaRepository);
        final var hibernateException = new ConstraintViolationException(
                "duplicate", new SQLException("duplicate", "23000", 1062), "uk_user_providers_identity_active_flag");
        when(jpaRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate", hibernateException));

        assertThatThrownBy(() -> adapter.save(UserProvider.create(42L, ProviderType.KAKAO, "123", null)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(UserErrorCode.PROVIDER_IDENTITY_CONFLICT));
    }
}
