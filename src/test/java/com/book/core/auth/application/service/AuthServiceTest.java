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
import com.book.core.user.application.command.IdentifyUserCommand;
import com.book.core.user.application.result.IdentifyUserResult;
import com.book.core.user.application.usecase.IdentifyUserUseCase;
import com.book.core.user.domain.ProviderType;
import java.time.Instant;
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
    IdentifyUserUseCase identifyUserUseCase;

    @Mock
    CreateLoginSessionUseCase createLoginSessionUseCase;

    @Mock
    AuthReissueUseCase authReissueUseCase;

    @Mock
    AuthLogoutUseCase authLogoutUseCase;

    @Test
    void OAuth_인증부터_refresh_session_저장까지_순서대로_실행한다() {
        final AuthLoginCommand command = command();
        when(authenticateOAuthUserUseCase.execute("authorization-code", "code-verifier", "nonce"))
            .thenReturn(new OAuthUserInfo("provider-123", "reader@example.com"));
        when(identifyUserUseCase.execute(new IdentifyUserCommand(ProviderType.KAKAO, "provider-123", "reader@example.com")))
            .thenReturn(new IdentifyUserResult(42L));
        final Instant refreshExpiresAt = Instant.parse("2030-01-09T03:04:05Z");
        when(createLoginSessionUseCase.execute(42L)).thenReturn(new AuthLoginResult("access-token", "refresh-token", refreshExpiresAt));

        final var result = service().login(command);

        final InOrder order = inOrder(authenticateOAuthUserUseCase, identifyUserUseCase, createLoginSessionUseCase);
        order.verify(authenticateOAuthUserUseCase).execute("authorization-code", "code-verifier", "nonce");
        order.verify(identifyUserUseCase).execute(new IdentifyUserCommand(ProviderType.KAKAO, "provider-123", "reader@example.com"));
        order.verify(createLoginSessionUseCase).execute(42L);
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.refreshExpiresAt()).isEqualTo(refreshExpiresAt);
    }

    @Test
    void OAuth_인증이_실패하면_회원_식별과_token_발급을_호출하지_않는다() {
        final AuthLoginCommand command = command();
        final BusinessException exception = new BusinessException(ErrorCode.INVALID_ID_TOKEN);
        when(authenticateOAuthUserUseCase.execute("authorization-code", "code-verifier", "nonce")).thenThrow(exception);

        assertThatThrownBy(() -> service().login(command)).isSameAs(exception);

        verifyNoInteractions(identifyUserUseCase, createLoginSessionUseCase);
    }

    @Test
    void 회원_식별이_실패하면_token_발급을_호출하지_않는다() {
        final AuthLoginCommand command = command();
        final BusinessException exception = new BusinessException(ErrorCode.USER_PROVIDER_USER_INACTIVE);
        when(authenticateOAuthUserUseCase.execute("authorization-code", "code-verifier", "nonce"))
            .thenReturn(new OAuthUserInfo("provider-123", null));
        when(identifyUserUseCase.execute(new IdentifyUserCommand(ProviderType.KAKAO, "provider-123", null))).thenThrow(exception);

        assertThatThrownBy(() -> service().login(command)).isSameAs(exception);

        verifyNoInteractions(createLoginSessionUseCase);
    }

    private AuthService service() {
        return new AuthService(authenticateOAuthUserUseCase, identifyUserUseCase, createLoginSessionUseCase, authReissueUseCase,
            authLogoutUseCase);
    }

    private AuthLoginCommand command() {
        return new AuthLoginCommand(ProviderType.KAKAO, "authorization-code", "code-verifier", "nonce");
    }
}
