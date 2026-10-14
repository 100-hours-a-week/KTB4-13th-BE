package com.book.core.user.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import com.book.core.cart.application.usecase.CreateCartUseCase;
import com.book.core.point.application.usecase.CreatePointBalanceUseCase;
import com.book.core.user.application.command.UserRegistrationCommand;
import com.book.core.user.application.port.NicknameGeneratorPort;
import com.book.core.user.application.port.UserProviderRepositoryPort;
import com.book.core.user.application.port.UserRepositoryPort;
import com.book.core.user.domain.ProviderType;
import com.book.core.user.domain.User;
import com.book.core.user.domain.UserProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserRegistrationUseCaseTest {
    @Mock
    UserRepositoryPort userRepository;

    @Mock
    UserProviderRepositoryPort userProviderRepository;

    @Mock
    CreateCartUseCase createCartUseCase;

    @Mock
    CreatePointBalanceUseCase createPointBalanceUseCase;

    @Mock
    NicknameGeneratorPort nicknameGenerator;

    @Test
    void 생성한_닉네임으로_User와_UserProvider를_저장한_뒤_Cart와_포인트_잔액을_순서대로_생성한다() {
        final UserRegistrationCommand command = new UserRegistrationCommand(ProviderType.KAKAO, "provider-123", null);
        when(nicknameGenerator.generate()).thenReturn("행복한독자1234");
        when(userRepository.save(any(User.class))).thenReturn(User.restore(42L, "행복한독자1234"));
        when(userProviderRepository.save(any(UserProvider.class)))
            .thenReturn(UserProvider.restore(7L, 42L, ProviderType.KAKAO, "provider-123", null));

        final Long userId = new UserRegistrationUseCase(userRepository, userProviderRepository, createCartUseCase,
            createPointBalanceUseCase, nicknameGenerator).execute(command);

        final var userCaptor = ArgumentCaptor.forClass(User.class);
        final var userProviderCaptor = ArgumentCaptor.forClass(UserProvider.class);
        final var order = inOrder(nicknameGenerator, userRepository, userProviderRepository, createCartUseCase, createPointBalanceUseCase);
        order.verify(nicknameGenerator).generate();
        order.verify(userRepository).save(userCaptor.capture());
        order.verify(userProviderRepository).save(userProviderCaptor.capture());
        order.verify(createCartUseCase).execute(42L);
        order.verify(createPointBalanceUseCase).execute(42L);
        assertThat(userId).isEqualTo(42L);
        assertThat(userCaptor.getValue().nickname()).isEqualTo("행복한독자1234");
        assertThat(userProviderCaptor.getValue().userId()).isEqualTo(42L);
        assertThat(userProviderCaptor.getValue().providerType()).isEqualTo(ProviderType.KAKAO);
        assertThat(userProviderCaptor.getValue().providerUserId()).isEqualTo("provider-123");
        assertThat(userProviderCaptor.getValue().providerEmail()).isNull();
    }
}
