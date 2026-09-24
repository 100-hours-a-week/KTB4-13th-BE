package com.book.core.user.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.BusinessException;
import com.book.core.user.application.command.FindOrRegisterUserCommand;
import com.book.core.user.application.command.UserRegistrationCommand;
import com.book.core.user.application.port.NicknameGenerator;
import com.book.core.user.application.port.UserProviderRepositoryPort;
import com.book.core.user.application.port.UserRepositoryPort;
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
class FindOrRegisterUserUseCaseTest {
    @Mock
    UserRepositoryPort userRepository;

    @Mock
    UserProviderRepositoryPort userProviderRepository;

    @Mock
    NicknameGenerator nicknameGenerator;

    @Mock
    UserRegistrationUseCase userRegistrationUseCase;

    @Test
    void 기존_활성_provider_연결의_내부_userId를_반환한다() {
        final FindOrRegisterUserCommand command =
                new FindOrRegisterUserCommand(ProviderType.KAKAO, "provider-123", null);
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
        final FindOrRegisterUserCommand command =
                new FindOrRegisterUserCommand(ProviderType.KAKAO, "missing-user", null);
        when(userProviderRepository.findActiveByProviderTypeAndProviderUserId(ProviderType.KAKAO, "missing-user"))
                .thenReturn(Optional.of(UserProvider.restore(7L, 42L, ProviderType.KAKAO, "missing-user", null, null)));
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().execute(command))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        (final var exception) -> assertThat(exception.errorCode())
                                .isEqualTo(UserErrorCode.USER_PROVIDER_USER_NOT_FOUND));
    }

    @Test
    void 기존_provider가_가리키는_User가_soft_delete_상태면_internal_error를_발생시킨다() {
        final FindOrRegisterUserCommand command =
                new FindOrRegisterUserCommand(ProviderType.KAKAO, "inactive-user", null);
        when(userProviderRepository.findActiveByProviderTypeAndProviderUserId(ProviderType.KAKAO, "inactive-user"))
                .thenReturn(
                        Optional.of(UserProvider.restore(7L, 42L, ProviderType.KAKAO, "inactive-user", null, null)));
        when(userRepository.findById(42L))
                .thenReturn(Optional.of(User.restore(42L, "북적이", LocalDateTime.of(2026, 9, 20, 0, 0))));

        assertThatThrownBy(() -> useCase().execute(command))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(UserErrorCode.USER_PROVIDER_USER_INACTIVE));
    }

    @Test
    void 신규_identity는_nullable_email과_생성한_nickname으로_가입을_시도한다() {
        final FindOrRegisterUserCommand command =
                new FindOrRegisterUserCommand(ProviderType.KAKAO, "new-provider", null);
        final UserRegistrationCommand registrationCommand =
                new UserRegistrationCommand(ProviderType.KAKAO, "new-provider", null, "행복한독자1234");
        when(userProviderRepository.findActiveByProviderTypeAndProviderUserId(ProviderType.KAKAO, "new-provider"))
                .thenReturn(Optional.empty());
        when(nicknameGenerator.generate()).thenReturn("행복한독자1234");
        when(userRegistrationUseCase.execute(registrationCommand)).thenReturn(42L);

        final var result = useCase().execute(command);

        assertThat(result.userId()).isEqualTo(42L);
        verify(userRegistrationUseCase).execute(registrationCommand);
    }

    @Test
    void 한_번의_nickname_충돌_후_재시도로_성공한다() {
        final FindOrRegisterUserCommand command = new FindOrRegisterUserCommand(ProviderType.KAKAO, "retry-once", null);
        when(userProviderRepository.findActiveByProviderTypeAndProviderUserId(ProviderType.KAKAO, "retry-once"))
                .thenReturn(Optional.empty());
        when(nicknameGenerator.generate()).thenReturn("고요한독자1000", "고요한독자1001");
        when(userRegistrationUseCase.execute(
                        new UserRegistrationCommand(ProviderType.KAKAO, "retry-once", null, "고요한독자1000")))
                .thenThrow(new BusinessException(UserErrorCode.NICKNAME_CONFLICT));
        when(userRegistrationUseCase.execute(
                        new UserRegistrationCommand(ProviderType.KAKAO, "retry-once", null, "고요한독자1001")))
                .thenReturn(42L);

        final var result = useCase().execute(command);

        assertThat(result.userId()).isEqualTo(42L);
        verify(nicknameGenerator, times(2)).generate();
    }

    @Test
    void 여러_번의_nickname_충돌_후_최대_시도_횟수_이내에_성공한다() {
        final FindOrRegisterUserCommand command = new FindOrRegisterUserCommand(ProviderType.KAKAO, "retry-many", null);
        when(userProviderRepository.findActiveByProviderTypeAndProviderUserId(ProviderType.KAKAO, "retry-many"))
                .thenReturn(Optional.empty());
        when(nicknameGenerator.generate()).thenReturn("고요한독자1000", "고요한독자1001", "고요한독자1002", "고요한독자1003", "고요한독자1004");
        when(userRegistrationUseCase.execute(
                        new UserRegistrationCommand(ProviderType.KAKAO, "retry-many", null, "고요한독자1000")))
                .thenThrow(new BusinessException(UserErrorCode.NICKNAME_CONFLICT));
        when(userRegistrationUseCase.execute(
                        new UserRegistrationCommand(ProviderType.KAKAO, "retry-many", null, "고요한독자1001")))
                .thenThrow(new BusinessException(UserErrorCode.NICKNAME_CONFLICT));
        when(userRegistrationUseCase.execute(
                        new UserRegistrationCommand(ProviderType.KAKAO, "retry-many", null, "고요한독자1002")))
                .thenThrow(new BusinessException(UserErrorCode.NICKNAME_CONFLICT));
        when(userRegistrationUseCase.execute(
                        new UserRegistrationCommand(ProviderType.KAKAO, "retry-many", null, "고요한독자1003")))
                .thenThrow(new BusinessException(UserErrorCode.NICKNAME_CONFLICT));
        when(userRegistrationUseCase.execute(
                        new UserRegistrationCommand(ProviderType.KAKAO, "retry-many", null, "고요한독자1004")))
                .thenReturn(42L);

        final var result = useCase().execute(command);

        assertThat(result.userId()).isEqualTo(42L);
        verify(nicknameGenerator, times(5)).generate();
    }

    @Test
    void 최대_시도_횟수를_초과하면_명시적인_서버_오류로_종료한다() {
        final FindOrRegisterUserCommand command =
                new FindOrRegisterUserCommand(ProviderType.KAKAO, "retry-exhausted", null);
        when(userProviderRepository.findActiveByProviderTypeAndProviderUserId(ProviderType.KAKAO, "retry-exhausted"))
                .thenReturn(Optional.empty());
        when(nicknameGenerator.generate()).thenReturn("고요한독자1000", "고요한독자1001", "고요한독자1002", "고요한독자1003", "고요한독자1004");
        when(userRegistrationUseCase.execute(any(UserRegistrationCommand.class)))
                .thenThrow(new BusinessException(UserErrorCode.NICKNAME_CONFLICT));

        assertThatThrownBy(() -> useCase().execute(command))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(UserErrorCode.NICKNAME_GENERATION_FAILED));
        verify(nicknameGenerator, times(5)).generate();
    }

    @Test
    void nickname_충돌이_아닌_예외는_재시도하지_않고_즉시_전달한다() {
        final FindOrRegisterUserCommand command =
                new FindOrRegisterUserCommand(ProviderType.KAKAO, "conflict-provider", null);
        when(userProviderRepository.findActiveByProviderTypeAndProviderUserId(ProviderType.KAKAO, "conflict-provider"))
                .thenReturn(Optional.empty());
        when(nicknameGenerator.generate()).thenReturn("행복한독자1234");
        when(userRegistrationUseCase.execute(
                        new UserRegistrationCommand(ProviderType.KAKAO, "conflict-provider", null, "행복한독자1234")))
                .thenThrow(new BusinessException(UserErrorCode.PROVIDER_IDENTITY_CONFLICT));

        assertThatThrownBy(() -> useCase().execute(command))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(UserErrorCode.PROVIDER_IDENTITY_CONFLICT));
        verify(nicknameGenerator).generate();
    }

    private FindOrRegisterUserUseCase useCase() {
        return new FindOrRegisterUserUseCase(
                userRepository, userProviderRepository, nicknameGenerator, userRegistrationUseCase);
    }
}
