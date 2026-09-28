package com.book.core.onboarding.infrastructure.client.ai;

import com.book.common.config.ai.AiRestClientConfig;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.common.logging.TraceIdFilter;
import com.book.core.onboarding.application.port.PersonalizationProfileClient;
import com.book.core.onboarding.application.port.PersonalizationProfileRequest;
import com.book.core.onboarding.application.port.PersonalizationProfileResult;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * {@code POST /preferences/profile} 연동. LLM을 쓰지 않는 동기 계산이라 기존 채팅 추천(30초)보다 짧은 공용
 * {@link AiRestClientConfig#DEFAULT_AI_REST_CLIENT}(5초) 타임아웃을 그대로 재사용한다.
 */
@Component
public class PersonalizationProfileClientImpl implements PersonalizationProfileClient {
    private static final String PROFILE_PATH = "/preferences/profile";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final RestClient restClient;

    public PersonalizationProfileClientImpl(@Qualifier(AiRestClientConfig.DEFAULT_AI_REST_CLIENT) final RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public PersonalizationProfileResult createProfile(final PersonalizationProfileRequest request) {
        final AiProfileRequest aiRequest = toAiProfileRequest(request);
        try {
            final AiProfileResponse response = restClient.post().uri(PROFILE_PATH).header(REQUEST_ID_HEADER, currentRequestId())
                .contentType(MediaType.APPLICATION_JSON).body(aiRequest).exchange((httpRequest, httpResponse) -> {
                    final HttpStatusCode status = httpResponse.getStatusCode();
                    if (status.is2xxSuccessful()) {
                        final AiProfileEnvelope envelope = httpResponse.bodyTo(AiProfileEnvelope.class);
                        if (envelope == null || envelope.data() == null) {
                            throw new CoreException(ErrorCode.AI_PROFILE_FAILURE);
                        }
                        return envelope.data();
                    }
                    if (status.value() == 401) {
                        throw new CoreException(ErrorCode.AI_SERVICE_UNAUTHORIZED);
                    }
                    if (status.value() == 400) {
                        throw new CoreException(ErrorCode.AI_PROFILE_INVALID_REQUEST);
                    }
                    if (status.value() == 409) {
                        throw new CoreException(ErrorCode.AI_PROFILE_IDEMPOTENCY_CONFLICT);
                    }
                    if (status.value() == 413) {
                        throw new CoreException(ErrorCode.AI_PROFILE_PAYLOAD_TOO_LARGE);
                    }
                    throw new CoreException(ErrorCode.AI_PROFILE_FAILURE);
                });
            return new PersonalizationProfileResult(response.coldStart(), response.profileVersion());
        } catch (final ResourceAccessException exception) {
            throw new CoreException(ErrorCode.AI_PROFILE_TIMEOUT, exception);
        } catch (final RestClientException exception) {
            throw new CoreException(ErrorCode.AI_PROFILE_FAILURE, exception);
        }
    }

    private String currentRequestId() {
        final String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
        return traceId != null ? traceId : UUID.randomUUID().toString();
    }

    private AiProfileRequest toAiProfileRequest(final PersonalizationProfileRequest request) {
        final AiProfileOnboarding onboarding = new AiProfileOnboarding(request.readingTimes(), request.criteria(), request.categories(),
            request.tags(), request.likedBookIds());
        return new AiProfileRequest(request.userId(), request.idempotencyKey(), onboarding);
    }
}
