package com.book.core.user.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.BusinessException;
import com.book.common.exception.ErrorCode;
import com.book.core.user.application.port.UserProviderRepositoryPort;
import com.book.core.user.application.port.UserRepositoryPort;
import com.book.core.user.application.result.OAuthUserResult;
import com.book.core.user.domain.ProviderType;
import com.book.core.user.domain.User;
import com.book.core.user.domain.UserProvider;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FindOAuthUserUseCaseTest {
    @Mock
    UserRepositoryPort userRepository;

    @Mock
    UserProviderRepositoryPort userProviderRepository;

    @Test
    void 기존_활성_provider_연결의_내부_userId를_반환한다() {
        when(userProviderRepository.findActiveByProviderTypeAndProviderUserId(ProviderType.KAKAO, "provider-123"))
            .thenReturn(Optional.of(UserProvider.restore(7L, 42L, ProviderType.KAKAO, "provider-123", null)));
        when(userRepository.findById(42L)).thenReturn(Optional.of(User.restore(42L, "북적이")));

        assertThat(useCase().execute(ProviderType.KAKAO, "provider-123")).contains(new OAuthUserResult(42L, false));
    }

    @Test
    void 기존_provider가_가리키는_User가_없으면_internal_error를_발생시킨다() {
        when(userProviderRepository.findActiveByProviderTypeAndProviderUserId(ProviderType.KAKAO, "missing-user"))
            .thenReturn(Optional.of(UserProvider.restore(7L, 42L, ProviderType.KAKAO, "missing-user", null)));
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().execute(ProviderType.KAKAO, "missing-user")).isInstanceOfSatisfying(BusinessException.class,
            (final var exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.USER_PROVIDER_USER_NOT_FOUND));
    }

    @Test
    void 기존_provider가_가리키는_User가_soft_delete_상태면_internal_error를_발생시킨다() {
        when(userProviderRepository.findActiveByProviderTypeAndProviderUserId(ProviderType.KAKAO, "inactive-user"))
            .thenReturn(Optional.of(UserProvider.restore(7L, 42L, ProviderType.KAKAO, "inactive-user", null)));
        final User inactiveUser = User.restore(42L, "북적이");
        inactiveUser.delete();
        when(userRepository.findById(42L)).thenReturn(Optional.of(inactiveUser));

        assertThatThrownBy(() -> useCase().execute(ProviderType.KAKAO, "inactive-user")).isInstanceOfSatisfying(BusinessException.class,
            (final var exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.USER_PROVIDER_USER_INACTIVE));
    }

    @Test
    void 활성_provider_연결이_없으면_빈_값을_반환한다() {
        when(userProviderRepository.findActiveByProviderTypeAndProviderUserId(ProviderType.KAKAO, "new-provider"))
            .thenReturn(Optional.empty());

        assertThat(useCase().execute(ProviderType.KAKAO, "new-provider")).isEmpty();
        verifyNoInteractions(userRepository);
    }

    @Test
    void 탈퇴_신청한_회원은_탈퇴_신청_상태와_기존_userId를_반환한다() {
        when(userProviderRepository.findActiveByProviderTypeAndProviderUserId(ProviderType.KAKAO, "withdrawal-provider"))
            .thenReturn(Optional.of(UserProvider.restore(7L, 42L, ProviderType.KAKAO, "withdrawal-provider", null)));
        final User user = User.restore(42L, "북적이");
        user.requestUserDeletion(LocalDateTime.of(2030, 1, 1, 0, 0));
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));

        assertThat(useCase().execute(ProviderType.KAKAO, "withdrawal-provider")).contains(new OAuthUserResult(42L, true));
    }

    private FindOAuthUserUseCase useCase() {
        return new FindOAuthUserUseCase(userRepository, userProviderRepository);
    }
}
