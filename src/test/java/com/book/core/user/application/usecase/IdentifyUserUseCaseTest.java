package com.book.core.user.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.BusinessException;
import com.book.common.exception.ErrorCode;
import com.book.core.user.application.command.IdentifyUserCommand;
import com.book.core.user.application.command.UserRegistrationCommand;
import com.book.core.user.application.port.UserProviderRepositoryPort;
import com.book.core.user.application.port.UserRepositoryPort;
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
class IdentifyUserUseCaseTest {
    @Mock
    UserRepositoryPort userRepository;

    @Mock
    UserProviderRepositoryPort userProviderRepository;

    @Mock
    UserRegistrationUseCase userRegistrationUseCase;

    @Mock
    RecoverWithdrawalUseCase recoverWithdrawalUseCase;

    @Test
    void 기존_활성_provider_연결의_내부_userId를_반환한다() {
        final IdentifyUserCommand command = new IdentifyUserCommand(ProviderType.KAKAO, "provider-123", null);
        when(userProviderRepository.findActiveByProviderTypeAndProviderUserId(ProviderType.KAKAO, "provider-123"))
            .thenReturn(Optional.of(UserProvider.restore(7L, 42L, ProviderType.KAKAO, "provider-123", null)));
        when(userRepository.findById(42L)).thenReturn(Optional.of(User.restore(42L, "북적이")));

        final var result = useCase().execute(command);

        assertThat(result.userId()).isEqualTo(42L);
        verify(userRepository).findById(42L);
        verifyNoInteractions(userRegistrationUseCase);
    }

    @Test
    void 기존_provider가_가리키는_User가_없으면_internal_error를_발생시킨다() {
        final IdentifyUserCommand command = new IdentifyUserCommand(ProviderType.KAKAO, "missing-user", null);
        when(userProviderRepository.findActiveByProviderTypeAndProviderUserId(ProviderType.KAKAO, "missing-user"))
            .thenReturn(Optional.of(UserProvider.restore(7L, 42L, ProviderType.KAKAO, "missing-user", null)));
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().execute(command)).isInstanceOfSatisfying(BusinessException.class,
            (final var exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.USER_PROVIDER_USER_NOT_FOUND));
    }

    @Test
    void 기존_provider가_가리키는_User가_soft_delete_상태면_internal_error를_발생시킨다() {
        final IdentifyUserCommand command = new IdentifyUserCommand(ProviderType.KAKAO, "inactive-user", null);
        when(userProviderRepository.findActiveByProviderTypeAndProviderUserId(ProviderType.KAKAO, "inactive-user"))
            .thenReturn(Optional.of(UserProvider.restore(7L, 42L, ProviderType.KAKAO, "inactive-user", null)));
        final User inactiveUser = User.restore(42L, "북적이");
        inactiveUser.delete();
        when(userRepository.findById(42L)).thenReturn(Optional.of(inactiveUser));

        assertThatThrownBy(() -> useCase().execute(command)).isInstanceOfSatisfying(BusinessException.class,
            (final var exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.USER_PROVIDER_USER_INACTIVE));
    }

    @Test
    void 신규_identity는_nullable_email로_가입을_시도한다() {
        final IdentifyUserCommand command = new IdentifyUserCommand(ProviderType.KAKAO, "new-provider", null);
        final UserRegistrationCommand registrationCommand = new UserRegistrationCommand(ProviderType.KAKAO, "new-provider", null);
        when(userProviderRepository.findActiveByProviderTypeAndProviderUserId(ProviderType.KAKAO, "new-provider"))
            .thenReturn(Optional.empty());
        when(userRegistrationUseCase.execute(registrationCommand)).thenReturn(42L);

        final var result = useCase().execute(command);

        assertThat(result.userId()).isEqualTo(42L);
        verify(userRegistrationUseCase).execute(registrationCommand);
    }

    @Test
    void 가입_실패_예외를_그대로_전달한다() {
        final IdentifyUserCommand command = new IdentifyUserCommand(ProviderType.KAKAO, "conflict-provider", null);
        when(userProviderRepository.findActiveByProviderTypeAndProviderUserId(ProviderType.KAKAO, "conflict-provider"))
            .thenReturn(Optional.empty());
        when(userRegistrationUseCase.execute(new UserRegistrationCommand(ProviderType.KAKAO, "conflict-provider", null)))
            .thenThrow(new BusinessException(ErrorCode.PROVIDER_IDENTITY_CONFLICT));

        assertThatThrownBy(() -> useCase().execute(command)).isInstanceOfSatisfying(BusinessException.class,
            (final var exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.PROVIDER_IDENTITY_CONFLICT));
        verify(userRegistrationUseCase).execute(new UserRegistrationCommand(ProviderType.KAKAO, "conflict-provider", null));
    }

    @Test
    void 탈퇴_신청한_회원이_로그인하면_복구를_요청하고_기존_userId를_반환한다() {
        final IdentifyUserCommand command = new IdentifyUserCommand(ProviderType.KAKAO, "withdrawal-provider", null);
        when(userProviderRepository.findActiveByProviderTypeAndProviderUserId(ProviderType.KAKAO, "withdrawal-provider"))
            .thenReturn(Optional.of(UserProvider.restore(7L, 42L, ProviderType.KAKAO, "withdrawal-provider", null)));
        final User user = User.restore(42L, "북적이");
        user.requestWithdrawal(LocalDateTime.of(2030, 1, 1, 0, 0));
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));

        assertThat(useCase().execute(command).userId()).isEqualTo(42L);

        verify(recoverWithdrawalUseCase).execute(42L);
        verifyNoInteractions(userRegistrationUseCase);
    }

    private IdentifyUserUseCase useCase() {
        return new IdentifyUserUseCase(userRepository, userProviderRepository, userRegistrationUseCase, recoverWithdrawalUseCase);
    }
}
