package com.book.core.auth.infrastructure.client.kakao;

import com.book.common.exception.BusinessException;
import com.book.core.auth.application.port.OAuthIdentity;
import com.book.core.auth.application.port.OAuthProviderClient;
import com.book.core.auth.domain.exception.AuthErrorCode;
import com.book.core.user.domain.ProviderType;
import com.nimbusds.jose.RemoteKeySourceException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class KakaoOAuthProviderClientImpl implements OAuthProviderClient {
    private static final String EMAIL_CLAIM = "email";

    private final JwtDecoder jwtDecoder;

    public KakaoOAuthProviderClientImpl(
            @Qualifier(KakaoOAuthClientConfiguration.KAKAO_JWT_DECODER) final JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public OAuthIdentity verify(final ProviderType providerType, final String idToken) {
        if (!StringUtils.hasText(idToken)) {
            throw new BusinessException(AuthErrorCode.INVALID_ID_TOKEN);
        }

        try {
            final Jwt jwt = jwtDecoder.decode(idToken);
            return toIdentity(jwt);
        } catch (final JwtException exception) {
            if (hasRemoteKeySourceCause(exception)) {
                throw new BusinessException(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE, exception);
            }
            throw new BusinessException(AuthErrorCode.INVALID_ID_TOKEN, exception);
        } catch (final IllegalArgumentException exception) {
            throw new BusinessException(AuthErrorCode.INVALID_ID_TOKEN, exception);
        }
    }

    private OAuthIdentity toIdentity(final Jwt jwt) {
        final String providerUserId = jwt.getSubject();
        if (!StringUtils.hasText(providerUserId) || jwt.getExpiresAt() == null) {
            throw new BusinessException(AuthErrorCode.INVALID_ID_TOKEN);
        }
        return new OAuthIdentity(providerUserId, jwt.getClaimAsString(EMAIL_CLAIM));
    }

    private boolean hasRemoteKeySourceCause(final Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof RemoteKeySourceException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
