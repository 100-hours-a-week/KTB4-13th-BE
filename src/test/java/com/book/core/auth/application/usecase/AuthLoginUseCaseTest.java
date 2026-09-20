package com.book.core.auth.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.BusinessException;
import com.book.common.exception.CommonErrorCode;
import com.book.core.auth.application.command.AuthLoginCommand;
import com.book.core.auth.application.port.IssuedTokens;
import com.book.core.auth.application.port.OAuthIdentity;
import com.book.core.auth.application.port.OAuthProviderClient;
import com.book.core.auth.application.port.OAuthTokenClient;
import com.book.core.auth.application.port.TokenIssuer;
import com.book.core.user.application.command.UserResolveCommand;
import com.book.core.user.application.result.UserResolveResult;
import com.book.core.user.application.usecase.UserResolveUseCase;
import com.book.core.user.domain.ProviderType;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthLoginUseCaseTest {
    @Mock
    OAuthTokenClient oAuthTokenClient;

    @Mock
    OAuthProviderClient oAuthProviderClient;

    @Mock
    UserResolveUseCase userResolveUseCase;

    @Mock
    TokenIssuer tokenIssuer;

    @Mock
    RefreshSessionRegistrationUseCase refreshSessionRegistrationUseCase;

    @Test
    void 인가_코드_교환부터_refresh_session_저장까지_순서대로_실행한다() {
        final AuthLoginCommand command = command();
        when(oAuthTokenClient.exchangeForIdToken(ProviderType.KAKAO, "authorization-code", "code-verifier"))
                .thenReturn("id-token");
        when(oAuthProviderClient.verify(ProviderType.KAKAO, "id-token", "nonce"))
                .thenReturn(new OAuthIdentity("provider-123", "reader@example.com"));
        when(userResolveUseCase.execute(
                        new UserResolveCommand(ProviderType.KAKAO, "provider-123", "reader@example.com")))
                .thenReturn(new UserResolveResult(42L));
        final Instant refreshExpiresAt = Instant.parse("2030-01-09T03:04:05Z");
        when(tokenIssuer.issue(42L)).thenReturn(new IssuedTokens("access-token", "refresh-token", refreshExpiresAt));

        final var result = useCase().execute(command);

        final var resolveCommand = ArgumentCaptor.forClass(UserResolveCommand.class);
        final InOrder order = inOrder(
                oAuthTokenClient,
                oAuthProviderClient,
                userResolveUseCase,
                tokenIssuer,
                refreshSessionRegistrationUseCase);
        order.verify(oAuthTokenClient).exchangeForIdToken(ProviderType.KAKAO, "authorization-code", "code-verifier");
        order.verify(oAuthProviderClient).verify(ProviderType.KAKAO, "id-token", "nonce");
        order.verify(userResolveUseCase).execute(resolveCommand.capture());
        assertThat(resolveCommand.getValue().providerType()).isEqualTo(ProviderType.KAKAO);
        assertThat(resolveCommand.getValue().providerUserId()).isEqualTo("provider-123");
        assertThat(resolveCommand.getValue().providerEmail()).isEqualTo("reader@example.com");
        order.verify(tokenIssuer).issue(42L);
        order.verify(refreshSessionRegistrationUseCase).execute(42L, "refresh-token", refreshExpiresAt);
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void nullable_providerEmail을_UserResolveCommand에_그대로_전달한다() {
        final AuthLoginCommand command = command();
        when(oAuthTokenClient.exchangeForIdToken(ProviderType.KAKAO, "authorization-code", "code-verifier"))
                .thenReturn("id-token");
        when(oAuthProviderClient.verify(ProviderType.KAKAO, "id-token", "nonce"))
                .thenReturn(new OAuthIdentity("provider-123", null));
        when(userResolveUseCase.execute(new UserResolveCommand(ProviderType.KAKAO, "provider-123", null)))
                .thenReturn(new UserResolveResult(42L));
        final Instant refreshExpiresAt = Instant.parse("2030-01-09T03:04:05Z");
        when(tokenIssuer.issue(42L)).thenReturn(new IssuedTokens("access-token", "refresh-token", refreshExpiresAt));

        useCase().execute(command);

        final var resolveCommand = ArgumentCaptor.forClass(UserResolveCommand.class);
        verify(userResolveUseCase).execute(resolveCommand.capture());
        assertThat(resolveCommand.getValue().providerEmail()).isNull();
    }

    @Test
    void 인가_코드_교환이_실패하면_ID_Token_검증과_후속_흐름을_호출하지_않는다() {
        final AuthLoginCommand command = command();
        final BusinessException exception = new BusinessException(CommonErrorCode.STORAGE_FAILURE);
        when(oAuthTokenClient.exchangeForIdToken(ProviderType.KAKAO, "authorization-code", "code-verifier"))
                .thenThrow(exception);

        assertThatThrownBy(() -> useCase().execute(command)).isSameAs(exception);

        verifyNoInteractions(oAuthProviderClient, userResolveUseCase, tokenIssuer, refreshSessionRegistrationUseCase);
    }

    @Test
    void ID_Token_검증이_실패하면_회원_resolve와_token_발급을_호출하지_않는다() {
        final AuthLoginCommand command = command();
        final BusinessException exception = new BusinessException(CommonErrorCode.STORAGE_FAILURE);
        when(oAuthTokenClient.exchangeForIdToken(ProviderType.KAKAO, "authorization-code", "code-verifier"))
                .thenReturn("invalid-id-token");
        when(oAuthProviderClient.verify(ProviderType.KAKAO, "invalid-id-token", "nonce"))
                .thenThrow(exception);

        assertThatThrownBy(() -> useCase().execute(command)).isSameAs(exception);

        verifyNoInteractions(userResolveUseCase, tokenIssuer, refreshSessionRegistrationUseCase);
    }

    @Test
    void 회원_resolve가_실패하면_token_발급을_호출하지_않는다() {
        final AuthLoginCommand command = command();
        final BusinessException exception = new BusinessException(CommonErrorCode.STORAGE_FAILURE);
        when(oAuthTokenClient.exchangeForIdToken(ProviderType.KAKAO, "authorization-code", "code-verifier"))
                .thenReturn("id-token");
        when(oAuthProviderClient.verify(ProviderType.KAKAO, "id-token", "nonce"))
                .thenReturn(new OAuthIdentity("provider-123", null));
        when(userResolveUseCase.execute(new UserResolveCommand(ProviderType.KAKAO, "provider-123", null)))
                .thenThrow(exception);

        assertThatThrownBy(() -> useCase().execute(command)).isSameAs(exception);

        verifyNoInteractions(tokenIssuer, refreshSessionRegistrationUseCase);
    }

    @Test
    void token_발급이_실패하면_refresh_session을_저장하지_않는다() {
        final AuthLoginCommand command = command();
        final BusinessException exception = new BusinessException(CommonErrorCode.STORAGE_FAILURE);
        when(oAuthTokenClient.exchangeForIdToken(ProviderType.KAKAO, "authorization-code", "code-verifier"))
                .thenReturn("id-token");
        when(oAuthProviderClient.verify(ProviderType.KAKAO, "id-token", "nonce"))
                .thenReturn(new OAuthIdentity("provider-123", null));
        when(userResolveUseCase.execute(new UserResolveCommand(ProviderType.KAKAO, "provider-123", null)))
                .thenReturn(new UserResolveResult(42L));
        when(tokenIssuer.issue(42L)).thenThrow(exception);

        assertThatThrownBy(() -> useCase().execute(command)).isSameAs(exception);

        verifyNoInteractions(refreshSessionRegistrationUseCase);
    }

    private AuthLoginUseCase useCase() {
        return new AuthLoginUseCase(
                oAuthTokenClient,
                oAuthProviderClient,
                userResolveUseCase,
                tokenIssuer,
                refreshSessionRegistrationUseCase);
    }

    private AuthLoginCommand command() {
        return new AuthLoginCommand(ProviderType.KAKAO, "authorization-code", "code-verifier", "nonce");
    }
}
