package com.book.core.user.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.book.common.exception.BusinessException;
import com.book.common.exception.ErrorCode;
import com.book.core.user.application.port.UserRepositoryPort;
import com.book.core.user.domain.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetMyInfoUseCaseTest {
    @Mock
    UserRepositoryPort userRepository;

    @Test
    void 활성_회원의_닉네임을_반환한다() {
        when(userRepository.findById(42L)).thenReturn(Optional.of(User.restore(42L, "책읽는북적이")));

        final var result = useCase().execute(42L);

        assertThat(result.nickname()).isEqualTo("책읽는북적이");
    }

    @Test
    void 회원이_없으면_USER_NOT_FOUND를_발생시킨다() {
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().execute(42L)).isInstanceOfSatisfying(BusinessException.class,
            (final var exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    @Test
    void 비활성_회원이면_UNAUTHORIZED를_발생시킨다() {
        final User inactiveUser = User.restore(42L, "책읽는북적이");
        inactiveUser.delete();
        when(userRepository.findById(42L)).thenReturn(Optional.of(inactiveUser));

        assertThatThrownBy(() -> useCase().execute(42L)).isInstanceOfSatisfying(BusinessException.class,
            (final var exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    private GetMyInfoUseCase useCase() {
        return new GetMyInfoUseCase(userRepository);
    }
}
