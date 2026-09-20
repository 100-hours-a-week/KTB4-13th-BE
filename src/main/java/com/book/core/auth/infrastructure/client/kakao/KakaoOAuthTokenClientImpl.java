package com.book.core.auth.infrastructure.client.kakao;

import com.book.common.exception.BusinessException;
import com.book.core.auth.application.port.OAuthTokenClient;
import com.book.core.auth.domain.exception.AuthErrorCode;
import com.book.core.user.domain.ProviderType;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class KakaoOAuthTokenClientImpl implements OAuthTokenClient {
    private static final String TOKEN_PATH = "/oauth/token";
    private static final String INVALID_AUTHORIZATION_CODE = "KOE320";
    private static final String RATE_LIMIT_EXCEEDED = "KOE237";
    private static final Set<String> CONFIGURATION_ERROR_CODES =
            Set.of("KOE009", "KOE010", "KOE101", "KOE114", "KOE126", "KOE127", "KOE303", "KOE310");

    private final RestClient restClient;
    private final KakaoProperties properties;

    public KakaoOAuthTokenClientImpl(
            @Qualifier(KakaoOAuthClientConfiguration.KAKAO_TOKEN_REST_CLIENT) final RestClient restClient,
            final KakaoProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public String exchangeForIdToken(
            final ProviderType providerType, final String authorizationCode, final String codeVerifier) {
        try {
            return restClient
                    .post()
                    .uri(TOKEN_PATH)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(tokenRequest(authorizationCode, codeVerifier))
                    .exchange((final var request, final var response) -> {
                        if (response.getStatusCode().is2xxSuccessful()) {
                            return extractIdToken(response.bodyTo(KakaoTokenResponse.class));
                        }
                        if (response.getStatusCode().is5xxServerError()) {
                            throw new BusinessException(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE);
                        }
                        if (response.getStatusCode().is4xxClientError()) {
                            throw clientError(response.bodyTo(KakaoTokenErrorResponse.class));
                        }
                        throw new BusinessException(AuthErrorCode.OAUTH_TOKEN_EXCHANGE_FAILURE);
                    });
        } catch (final ResourceAccessException exception) {
            throw new BusinessException(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE, exception);
        } catch (final RestClientException exception) {
            throw new BusinessException(AuthErrorCode.OAUTH_TOKEN_EXCHANGE_FAILURE, exception);
        }
    }

    private MultiValueMap<String, String> tokenRequest(final String authorizationCode, final String codeVerifier) {
        final MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", properties.restApiKey());
        if (StringUtils.hasText(properties.clientSecret())) {
            form.add("client_secret", properties.clientSecret());
        }
        form.add("redirect_uri", properties.redirectUri());
        form.add("code", authorizationCode);
        form.add("code_verifier", codeVerifier);
        return form;
    }

    private String extractIdToken(final KakaoTokenResponse response) {
        if (response == null || !StringUtils.hasText(response.idToken())) {
            throw new BusinessException(AuthErrorCode.OAUTH_PROVIDER_CONFIGURATION_ERROR);
        }
        return response.idToken();
    }

    private BusinessException clientError(final KakaoTokenErrorResponse response) {
        if (response == null || !StringUtils.hasText(response.errorCode())) {
            return new BusinessException(AuthErrorCode.OAUTH_TOKEN_EXCHANGE_FAILURE);
        }
        if (INVALID_AUTHORIZATION_CODE.equals(response.errorCode())) {
            return new BusinessException(AuthErrorCode.INVALID_AUTHORIZATION_CODE);
        }
        if (RATE_LIMIT_EXCEEDED.equals(response.errorCode())) {
            return new BusinessException(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE);
        }
        if (CONFIGURATION_ERROR_CODES.contains(response.errorCode())) {
            return new BusinessException(AuthErrorCode.OAUTH_PROVIDER_CONFIGURATION_ERROR);
        }
        return new BusinessException(AuthErrorCode.OAUTH_TOKEN_EXCHANGE_FAILURE);
    }
}
