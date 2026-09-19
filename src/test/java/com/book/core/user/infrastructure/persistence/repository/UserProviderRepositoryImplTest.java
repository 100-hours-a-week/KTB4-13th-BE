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
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

class UserProviderRepositoryImplTest {
    @Test
    void 저장과_활성_조회에서_발생한_기술_예외를_공통_계약으로_변환한다() {
        final UserProviderJpaRepository jpaRepository = mock(UserProviderJpaRepository.class);
        final var adapter = new UserProviderRepositoryImpl(jpaRepository);
        when(jpaRepository.saveAndFlush(any())).thenThrow(new DataAccessResourceFailureException("unavailable"));
        when(jpaRepository.findActiveByProviderTypeAndProviderUserId("KAKAO", "123"))
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
}
