package com.book.core.user.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.BusinessException;
import com.book.common.exception.ErrorCode;
import com.book.core.user.application.command.UserResolveCommand;
import com.book.core.user.application.port.NicknameGenerator;
import com.book.core.user.application.port.UserProviderRepository;
import com.book.core.user.application.port.UserRepository;
import com.book.core.user.domain.ProviderType;
import com.book.core.user.domain.User;
import com.book.core.user.domain.UserProvider;
import com.book.core.user.domain.exception.UserErrorCode;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserResolveUseCaseTest {
    @Mock
    UserRepository userRepository;

    @Mock
    UserProviderRepository userProviderRepository;

    @Mock
    NicknameGenerator nicknameGenerator;

    @Mock
    UserRegistrationUseCase userRegistrationUseCase;

    @Test
    void 기존_활성_provider_연결의_내부_userId를_반환한다() {
        final UserResolveCommand command = new UserResolveCommand(ProviderType.KAKAO, "provider-123", null);
        when(userProviderRepository.findActiveByProviderTypeAndProviderUserId(ProviderType.KAKAO, "provider-123"))
                .thenReturn(Optional.of(UserProvider.restore(7L, 42L, ProviderType.KAKAO, "provider-123", null, null)));
        when(userRepository.findById(42L)).thenReturn(Optional.of(User.restore(42L, "북적이", null)));

        final var result = useCase().execute(command);

        assertThat(result.userId()).isEqualTo(42L);
        verify(userRepository).findById(42L);
        verifyNoInteractions(nicknameGenerator, userRegistrationUseCase);
    }

    @Test
    void 기존_provider가_가리키는_User가_없으면_internal_error를_발생시킨다() {
        final UserResolveCommand command = new UserResolveCommand(ProviderType.KAKAO, "missing-user", null);
        when(userProviderRepository.findActiveByProviderTypeAndProviderUserId(ProviderType.KAKAO, "missing-user"))
                .thenReturn(Optional.of(UserProvider.restore(7L, 42L, ProviderType.KAKAO, "missing-user", null, null)));
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().execute(command))
                .isInstanceOfSatisfying(BusinessException.class, (final var exception) -> {
                    assertThat(exception.errorCode()).isEqualTo(UserErrorCode.USER_PROVIDER_USER_NOT_FOUND);
                    assertThat(exception.errorCode().category()).isEqualTo(ErrorCode.Category.INTERNAL_ERROR);
                });
    }

    @Test
    void 기존_provider가_가리키는_User가_soft_delete_상태면_internal_error를_발생시킨다() {
        final UserResolveCommand command = new UserResolveCommand(ProviderType.KAKAO, "inactive-user", null);
        when(userProviderRepository.findActiveByProviderTypeAndProviderUserId(ProviderType.KAKAO, "inactive-user"))
                .thenReturn(
                        Optional.of(UserProvider.restore(7L, 42L, ProviderType.KAKAO, "inactive-user", null, null)));
        when(userRepository.findById(42L))
                .thenReturn(Optional.of(User.restore(42L, "북적이", LocalDateTime.of(2026, 9, 20, 0, 0))));

        assertThatThrownBy(() -> useCase().execute(command))
                .isInstanceOfSatisfying(BusinessException.class, (final var exception) -> {
                    assertThat(exception.errorCode()).isEqualTo(UserErrorCode.USER_PROVIDER_USER_INACTIVE);
                    assertThat(exception.errorCode().category()).isEqualTo(ErrorCode.Category.INTERNAL_ERROR);
                });
    }

    @Test
    void 신규_identity는_nullable_email과_생성한_nickname으로_가입을_시도한다() {
        final UserResolveCommand command = new UserResolveCommand(ProviderType.KAKAO, "new-provider", null);
        when(userProviderRepository.findActiveByProviderTypeAndProviderUserId(ProviderType.KAKAO, "new-provider"))
                .thenReturn(Optional.empty());
        when(nicknameGenerator.generate()).thenReturn("행복한독자1234");
        when(userRegistrationUseCase.execute(command, "행복한독자1234")).thenReturn(42L);

        final var result = useCase().execute(command);

        assertThat(result.userId()).isEqualTo(42L);
        assertThat(command.providerEmail()).isNull();
        verify(userRegistrationUseCase).execute(command, "행복한독자1234");
    }

    @Test
    void nickname_충돌은_제한_없이_새로운_가입_시도를_호출한다() {
        final UserResolveCommand command = new UserResolveCommand(ProviderType.KAKAO, "retry-provider", null);
        when(userProviderRepository.findActiveByProviderTypeAndProviderUserId(ProviderType.KAKAO, "retry-provider"))
                .thenReturn(Optional.empty());
        when(nicknameGenerator.generate()).thenReturn("고요한독자1000", "고요한독자1001", "고요한독자1002");
        when(userRegistrationUseCase.execute(command, "고요한독자1000"))
                .thenThrow(new BusinessException(UserErrorCode.NICKNAME_CONFLICT));
        when(userRegistrationUseCase.execute(command, "고요한독자1001"))
                .thenThrow(new BusinessException(UserErrorCode.NICKNAME_CONFLICT));
        when(userRegistrationUseCase.execute(command, "고요한독자1002")).thenReturn(42L);

        final var result = useCase().execute(command);

        assertThat(result.userId()).isEqualTo(42L);
        verify(nicknameGenerator, times(3)).generate();
        verify(userRegistrationUseCase).execute(command, "고요한독자1000");
        verify(userRegistrationUseCase).execute(command, "고요한독자1001");
        verify(userRegistrationUseCase).execute(command, "고요한독자1002");
    }

    @Test
    void provider_identity_충돌은_재시도하지_않고_전달한다() {
        final UserResolveCommand command = new UserResolveCommand(ProviderType.KAKAO, "conflict-provider", null);
        when(userProviderRepository.findActiveByProviderTypeAndProviderUserId(ProviderType.KAKAO, "conflict-provider"))
                .thenReturn(Optional.empty());
        when(nicknameGenerator.generate()).thenReturn("행복한독자1234");
        when(userRegistrationUseCase.execute(command, "행복한독자1234"))
                .thenThrow(new BusinessException(UserErrorCode.PROVIDER_IDENTITY_CONFLICT));

        assertThatThrownBy(() -> useCase().execute(command))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(UserErrorCode.PROVIDER_IDENTITY_CONFLICT));
        verify(nicknameGenerator).generate();
        verify(userRegistrationUseCase).execute(command, "행복한독자1234");
    }

    private UserResolveUseCase useCase() {
        return new UserResolveUseCase(
                userRepository, userProviderRepository, nicknameGenerator, userRegistrationUseCase);
    }
}
