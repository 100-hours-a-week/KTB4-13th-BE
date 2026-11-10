package com.book.core.onboarding.infrastructure.client.ai;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.common.filter.RequestMdcFilter;
import com.book.core.onboarding.application.port.AiPersonalizationProfileClient;
import com.book.core.onboarding.application.port.AiPersonalizationProfileRequest;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class AiPersonalizationProfileClientImpl implements AiPersonalizationProfileClient {
    private static final String PROFILE_PATH = "/preferences/profile";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final RestClient restClient;
    private final AiPersonalizationProfileExternalMapper mapper;

    public AiPersonalizationProfileClientImpl(@Qualifier("recommendationAiRestClient") final RestClient restClient,
        final AiPersonalizationProfileExternalMapper mapper) {
        this.restClient = restClient;
        this.mapper = mapper;
    }

    @Override
    public void updateProfile(final AiPersonalizationProfileRequest request) {
        try {
            restClient.put().uri(PROFILE_PATH).header(REQUEST_ID_HEADER, currentRequestId()).contentType(MediaType.APPLICATION_JSON)
                .body(mapper.toProfileRequest(request)).retrieve().toBodilessEntity();
        } catch (final RestClientException exception) {
            throw new CoreException(ErrorCode.AI_SERVICE_FAILURE, exception);
        }
    }

    @Override
    public void deleteProfile(final Long userId) {
        try {
            restClient.delete().uri(uriBuilder -> uriBuilder.path(PROFILE_PATH).queryParam("user_id", userId).build())
                .header(REQUEST_ID_HEADER, currentRequestId()).retrieve().toBodilessEntity();
        } catch (final HttpClientErrorException.BadRequest exception) {
            throw new CoreException(ErrorCode.AI_SERVICE_INVALID_REQUEST, exception);
        } catch (final RestClientException exception) {
            throw new CoreException(ErrorCode.AI_SERVICE_FAILURE, exception);
        }
    }

    private String currentRequestId() {
        final String requestId = MDC.get(RequestMdcFilter.REQUEST_ID_MDC_KEY);
        return requestId != null ? requestId : UUID.randomUUID().toString();
    }
}
