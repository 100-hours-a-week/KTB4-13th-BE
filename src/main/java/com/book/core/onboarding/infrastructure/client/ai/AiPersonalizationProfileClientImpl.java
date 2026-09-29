package com.book.core.onboarding.infrastructure.client.ai;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.common.logging.TraceIdFilter;
import com.book.core.onboarding.application.port.AiPersonalizationProfileClient;
import com.book.core.onboarding.application.port.AiPersonalizationProfileRequest;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class AiPersonalizationProfileClientImpl implements AiPersonalizationProfileClient {
    private static final String PROFILE_PATH = "/preferences/profile";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final RestClient restClient;
    private final PersonalizationProfileIdempotencyKeyGenerator idempotencyKeyGenerator;

    public AiPersonalizationProfileClientImpl(@Qualifier("aiRestClient") final RestClient restClient,
        final PersonalizationProfileIdempotencyKeyGenerator idempotencyKeyGenerator) {
        this.restClient = restClient;
        this.idempotencyKeyGenerator = idempotencyKeyGenerator;
    }

    @Override
    public void createProfile(final AiPersonalizationProfileRequest request) {
        try {
            restClient.post().uri(PROFILE_PATH).header(REQUEST_ID_HEADER, currentRequestId()).contentType(MediaType.APPLICATION_JSON)
                .body(AiProfileRequest.from(request, idempotencyKeyGenerator.generate(request))).retrieve().toBodilessEntity();
        } catch (final RestClientException exception) {
            throw new CoreException(ErrorCode.AI_SERVICE_FAILURE, exception);
        }
    }

    private String currentRequestId() {
        final String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
        return traceId != null ? traceId : UUID.randomUUID().toString();
    }
}
