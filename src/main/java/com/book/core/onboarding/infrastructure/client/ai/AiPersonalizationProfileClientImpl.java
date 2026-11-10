package com.book.core.onboarding.infrastructure.client.ai;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.common.filter.RequestMdcFilter;
import com.book.core.book.application.usecase.GetAiBookIdentifiersUseCase;
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
    private final PersonalizationProfileIdempotencyKeyGenerator idempotencyKeyGenerator;
    private final GetAiBookIdentifiersUseCase identifiers;

    public AiPersonalizationProfileClientImpl(@Qualifier("recommendationAiRestClient") final RestClient restClient,
        final PersonalizationProfileIdempotencyKeyGenerator idempotencyKeyGenerator, final GetAiBookIdentifiersUseCase identifiers) {
        this.restClient = restClient;
        this.idempotencyKeyGenerator = idempotencyKeyGenerator;
        this.identifiers = identifiers;
    }

    @Override
    public void updateProfile(final AiPersonalizationProfileRequest request) {
        try {
            restClient.put().uri(PROFILE_PATH).header(REQUEST_ID_HEADER, currentRequestId()).contentType(MediaType.APPLICATION_JSON).body(
                AiProfileRequest.from(request, idempotencyKeyGenerator.generate(request), identifiers.findIsbns(request.likedBookIds())))
                .retrieve().toBodilessEntity();
        } catch (final RestClientException exception) {
            throw failureOf(exception);
        }
    }

    @Override
    public void deleteProfile(final Long userId) {
        try {
            restClient.delete().uri(uriBuilder -> uriBuilder.path(PROFILE_PATH).queryParam("user_id", userId).build())
                .header(REQUEST_ID_HEADER, currentRequestId()).retrieve().toBodilessEntity();
        } catch (final RestClientException exception) {
            throw failureOf(exception);
        }
    }

    /** 400은 같은 요청을 다시 보내도 결과가 같으므로 재시도 대상인 일반 실패와 구분합니다. */
    private static CoreException failureOf(final RestClientException exception) {
        if (exception instanceof final HttpClientErrorException.BadRequest badRequest) {
            return new CoreException(ErrorCode.AI_SERVICE_INVALID_REQUEST, badRequest);
        }
        return new CoreException(ErrorCode.AI_SERVICE_FAILURE, exception);
    }

    private String currentRequestId() {
        final String requestId = MDC.get(RequestMdcFilter.REQUEST_ID_MDC_KEY);
        return requestId != null ? requestId : UUID.randomUUID().toString();
    }
}
