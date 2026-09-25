package com.book.core.user.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.core.user.application.command.UserRegistrationCommand;
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

    @Test
    void 저장된_User_ID와_nullable_email로_UserProvider를_저장한다() {
        final UserRegistrationCommand command =
                new UserRegistrationCommand(ProviderType.KAKAO, "provider-123", null, "행복한독자1234");
        when(userRepository.save(any(User.class))).thenReturn(User.restore(42L, "행복한독자1234", null));
        when(userProviderRepository.save(any(UserProvider.class)))
                .thenReturn(UserProvider.restore(7L, 42L, ProviderType.KAKAO, "provider-123", null, null));

        final Long userId = new UserRegistrationUseCase(userRepository, userProviderRepository).execute(command);

        final var userProviderCaptor = ArgumentCaptor.forClass(UserProvider.class);
        verify(userProviderRepository).save(userProviderCaptor.capture());
        assertThat(userId).isEqualTo(42L);
        assertThat(userProviderCaptor.getValue().userId()).isEqualTo(42L);
        assertThat(userProviderCaptor.getValue().providerType()).isEqualTo(ProviderType.KAKAO);
        assertThat(userProviderCaptor.getValue().providerUserId()).isEqualTo("provider-123");
        assertThat(userProviderCaptor.getValue().providerEmail()).isNull();
    }
}
