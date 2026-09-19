package com.book.core.auth.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.book.common.exception.BusinessException;
import com.book.common.exception.CommonErrorCode;
import com.book.core.auth.domain.RefreshSession;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

class RefreshSessionRepositoryImplTest {
    @Test
    void 저장과_조회의_기술_예외를_공통_저장소_오류로_변환한다() {
        final RefreshSessionJpaRepository jpaRepository = mock(RefreshSessionJpaRepository.class);
        final var adapter = new RefreshSessionRepositoryImpl(jpaRepository);
        final var failure = new DataAccessResourceFailureException("unavailable");
        when(jpaRepository.saveAndFlush(any())).thenThrow(failure);
        when(jpaRepository.findByUserIdAndRevokedAtIsNull(42L)).thenThrow(failure);
        when(jpaRepository.findByTokenHashAndRevokedAtIsNull("token-hash")).thenThrow(failure);
        final RefreshSession session = RefreshSession.create(42L, "token-hash", Instant.parse("2030-01-09T03:04:05Z"));

        assertStorageFailure(() -> adapter.save(session));
        assertStorageFailure(() -> adapter.findActiveByUserId(42L));
        assertStorageFailure(() -> adapter.findActiveByTokenHash("token-hash"));
    }

    private void assertStorageFailure(final Runnable operation) {
        assertThatThrownBy(operation::run)
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(CommonErrorCode.STORAGE_FAILURE));
    }
}
