package com.book.core.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.BusinessException;
import com.book.common.exception.ErrorCode;
import com.book.core.auth.application.command.AuthLoginCommand;
import com.book.core.auth.application.result.AuthLoginResult;
import com.book.core.auth.application.result.OAuthUserInfo;
import com.book.core.auth.application.usecase.AuthLogoutUseCase;
import com.book.core.auth.application.usecase.AuthReissueUseCase;
import com.book.core.auth.application.usecase.AuthenticateOAuthUserUseCase;
import com.book.core.auth.application.usecase.CreateLoginSessionUseCase;
import com.book.core.cart.application.usecase.CreateCartUseCase;
import com.book.core.point.application.usecase.CreatePointBalanceUseCase;
import com.book.core.user.application.command.UserRegistrationCommand;
import com.book.core.user.application.usecase.FindOAuthUserUseCase;
import com.book.core.user.application.usecase.UserRegistrationUseCase;
import com.book.core.user.domain.ProviderType;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    AuthenticateOAuthUserUseCase authenticateOAuthUserUseCase;

    @Mock
    FindOAuthUserUseCase findOAuthUserUseCase;

    @Mock
    UserRegistrationUseCase userRegistrationUseCase;

    @Mock
    CreateCartUseCase createCartUseCase;

    @Mock
    CreatePointBalanceUseCase createPointBalanceUseCase;

    @Mock
    CreateLoginSessionUseCase createLoginSessionUseCase;

    @Mock
    AuthReissueUseCase authReissueUseCase;

    @Mock
    AuthLogoutUseCase authLogoutUseCase;

    @Test
    void 기존_회원은_가입과_초기화_없이_OAuth_인증부터_refresh_session_저장까지_순서대로_실행한다() {
        final AuthLoginCommand command = command();
        when(authenticateOAuthUserUseCase.execute("authorization-code", "code-verifier", "nonce"))
            .thenReturn(new OAuthUserInfo("provider-123", "reader@example.com"));
        when(findOAuthUserUseCase.execute(ProviderType.KAKAO, "provider-123")).thenReturn(Optional.of(42L));
        final Instant refreshExpiresAt = Instant.parse("2030-01-09T03:04:05Z");
        when(createLoginSessionUseCase.execute(42L)).thenReturn(new AuthLoginResult("access-token", "refresh-token", refreshExpiresAt));

        final var result = service().login(command);

        final InOrder order = inOrder(authenticateOAuthUserUseCase, findOAuthUserUseCase, createLoginSessionUseCase);
        order.verify(authenticateOAuthUserUseCase).execute("authorization-code", "code-verifier", "nonce");
        order.verify(findOAuthUserUseCase).execute(ProviderType.KAKAO, "provider-123");
        order.verify(createLoginSessionUseCase).execute(42L);
        verifyNoInteractions(userRegistrationUseCase, createCartUseCase, createPointBalanceUseCase);
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.refreshExpiresAt()).isEqualTo(refreshExpiresAt);
    }

    @Test
    void 신규_회원은_등록과_장바구니_포인트_초기화_후_token을_발급한다() {
        final AuthLoginCommand command = command();
        final UserRegistrationCommand registrationCommand =
            new UserRegistrationCommand(ProviderType.KAKAO, "provider-123", "reader@example.com");
        when(authenticateOAuthUserUseCase.execute("authorization-code", "code-verifier", "nonce"))
            .thenReturn(new OAuthUserInfo("provider-123", "reader@example.com"));
        when(findOAuthUserUseCase.execute(ProviderType.KAKAO, "provider-123")).thenReturn(Optional.empty());
        when(userRegistrationUseCase.execute(registrationCommand)).thenReturn(42L);
        final Instant refreshExpiresAt = Instant.parse("2030-01-09T03:04:05Z");
        when(createLoginSessionUseCase.execute(42L)).thenReturn(new AuthLoginResult("access-token", "refresh-token", refreshExpiresAt));

        service().login(command);

        final InOrder order =
            inOrder(userRegistrationUseCase, createCartUseCase, createPointBalanceUseCase, createLoginSessionUseCase);
        order.verify(userRegistrationUseCase).execute(registrationCommand);
        order.verify(createCartUseCase).execute(42L);
        order.verify(createPointBalanceUseCase).execute(42L);
        order.verify(createLoginSessionUseCase).execute(42L);
    }

    @Test
    void OAuth_인증이_실패하면_회원_조회와_token_발급을_호출하지_않는다() {
        final AuthLoginCommand command = command();
        final BusinessException exception = new BusinessException(ErrorCode.INVALID_ID_TOKEN);
        when(authenticateOAuthUserUseCase.execute("authorization-code", "code-verifier", "nonce")).thenThrow(exception);

        assertThatThrownBy(() -> service().login(command)).isSameAs(exception);

        verifyNoInteractions(findOAuthUserUseCase, userRegistrationUseCase, createLoginSessionUseCase);
    }

    @Test
    void 회원_조회가_실패하면_가입과_token_발급을_호출하지_않는다() {
        final AuthLoginCommand command = command();
        final BusinessException exception = new BusinessException(ErrorCode.USER_PROVIDER_USER_INACTIVE);
        when(authenticateOAuthUserUseCase.execute("authorization-code", "code-verifier", "nonce"))
            .thenReturn(new OAuthUserInfo("provider-123", null));
        when(findOAuthUserUseCase.execute(ProviderType.KAKAO, "provider-123")).thenThrow(exception);

        assertThatThrownBy(() -> service().login(command)).isSameAs(exception);

        verifyNoInteractions(userRegistrationUseCase, createLoginSessionUseCase);
    }

    @Test
    void 회원_등록이_실패하면_초기화와_token_발급을_호출하지_않는다() {
        final AuthLoginCommand command = command();
        final BusinessException exception = new BusinessException(ErrorCode.PROVIDER_IDENTITY_CONFLICT);
        when(authenticateOAuthUserUseCase.execute("authorization-code", "code-verifier", "nonce"))
            .thenReturn(new OAuthUserInfo("provider-123", null));
        when(findOAuthUserUseCase.execute(ProviderType.KAKAO, "provider-123")).thenReturn(Optional.empty());
        when(userRegistrationUseCase.execute(new UserRegistrationCommand(ProviderType.KAKAO, "provider-123", null))).thenThrow(exception);

        assertThatThrownBy(() -> service().login(command)).isSameAs(exception);

        verifyNoInteractions(createCartUseCase, createPointBalanceUseCase, createLoginSessionUseCase);
    }

    private AuthService service() {
        return new AuthService(authenticateOAuthUserUseCase, findOAuthUserUseCase, userRegistrationUseCase, createCartUseCase,
            createPointBalanceUseCase, createLoginSessionUseCase, authReissueUseCase, authLogoutUseCase);
    }

    private AuthLoginCommand command() {
        return new AuthLoginCommand(ProviderType.KAKAO, "authorization-code", "code-verifier", "nonce");
    }
}
