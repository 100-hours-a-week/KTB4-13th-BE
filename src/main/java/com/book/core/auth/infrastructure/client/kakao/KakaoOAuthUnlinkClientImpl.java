package com.book.core.auth.infrastructure.client.kakao;

import com.book.common.exception.BusinessException;
import com.book.common.exception.ErrorCode;
import com.book.core.user.application.port.OAuthUnlinkClient;
import com.book.core.user.domain.ProviderType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class KakaoOAuthUnlinkClientImpl implements OAuthUnlinkClient {
    private static final String UNLINK_PATH = "/v1/user/unlink";
    private static final int NOT_LINKED_USER = -101;

    private final RestClient restClient;
    private final KakaoProperties properties;

    public KakaoOAuthUnlinkClientImpl(@Qualifier(KakaoUnlinkClientConfiguration.KAKAO_API_REST_CLIENT) final RestClient restClient,
        final KakaoProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public void unlink(final ProviderType providerType, final String providerUserId) {
        if (providerType != ProviderType.KAKAO || !StringUtils.hasText(properties.adminKey())) {
            throw new BusinessException(ErrorCode.OAUTH_PROVIDER_CONFIGURATION_ERROR);
        }
        try {
            restClient.post().uri(UNLINK_PATH).header(HttpHeaders.AUTHORIZATION, "KakaoAK " + properties.adminKey())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(unlinkRequest(providerUserId))
                .exchange((final var request, final var response) -> {
                    if (response.getStatusCode().is2xxSuccessful()) {
                        verifyUnlinkedUser(response.bodyTo(KakaoUnlinkResponse.class), providerUserId);
                        return null;
                    }
                    if (response.getStatusCode().is4xxClientError() && isNotLinkedUser(response.bodyTo(KakaoUnlinkErrorResponse.class))) {
                        return null;
                    }
                    if (response.getStatusCode().is5xxServerError()) {
                        throw new BusinessException(ErrorCode.OAUTH_PROVIDER_UNAVAILABLE);
                    }
                    throw new BusinessException(ErrorCode.OAUTH_PROVIDER_CONFIGURATION_ERROR);
                });
        } catch (final ResourceAccessException exception) {
            throw new BusinessException(ErrorCode.OAUTH_PROVIDER_UNAVAILABLE, exception);
        } catch (final RestClientException exception) {
            throw new BusinessException(ErrorCode.OAUTH_PROVIDER_CONFIGURATION_ERROR, exception);
        }
    }

    private MultiValueMap<String, String> unlinkRequest(final String providerUserId) {
        final MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("target_id_type", "user_id");
        form.add("target_id", providerUserId);
        return form;
    }

    private void verifyUnlinkedUser(final KakaoUnlinkResponse response, final String providerUserId) {
        if (response == null || response.id() == null || !providerUserId.equals(String.valueOf(response.id()))) {
            throw new BusinessException(ErrorCode.OAUTH_PROVIDER_CONFIGURATION_ERROR);
        }
    }

    private boolean isNotLinkedUser(final KakaoUnlinkErrorResponse response) {
        return response != null && Integer.valueOf(NOT_LINKED_USER).equals(response.code());
    }
}
