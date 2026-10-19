package com.book.core.user.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.book.common.exception.BusinessException;
import com.book.common.exception.ErrorCode;
import com.book.core.user.application.command.UpdateMyInfoCommand;
import com.book.core.user.application.port.UserRepositoryPort;
import com.book.core.user.domain.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateMyInfoUseCaseTest {
    @Mock
    UserRepositoryPort userRepository;

    @Test
    void 활성_회원의_닉네임을_변경한다() {
        final User user = User.restore(42L, "북적이");
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));

        useCase().execute(new UpdateMyInfoCommand(42L, "새로운북적이"));

        assertThat(user.nickname()).isEqualTo("새로운북적이");
    }

    @Test
    void 회원이_없으면_USER_NOT_FOUND를_발생시킨다() {
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().execute(new UpdateMyInfoCommand(42L, "새로운북적이"))).isInstanceOfSatisfying(BusinessException.class,
            (final var exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    @Test
    void 비활성_회원이면_UNAUTHORIZED를_발생시키고_닉네임을_변경하지_않는다() {
        final User inactiveUser = User.restore(42L, "북적이");
        inactiveUser.delete();
        when(userRepository.findById(42L)).thenReturn(Optional.of(inactiveUser));

        assertThatThrownBy(() -> useCase().execute(new UpdateMyInfoCommand(42L, "새로운북적이"))).isInstanceOfSatisfying(BusinessException.class,
            (final var exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
        assertThat(inactiveUser.nickname()).isEqualTo("북적이");
    }

    private UpdateMyInfoUseCase useCase() {
        return new UpdateMyInfoUseCase(userRepository);
    }
}
