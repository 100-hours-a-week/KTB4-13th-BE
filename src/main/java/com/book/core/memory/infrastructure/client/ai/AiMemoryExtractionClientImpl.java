package com.book.core.memory.infrastructure.client.ai;

import com.book.common.config.ai.AiRestClientConfig;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.common.exception.RetryAfterException;
import com.book.common.filter.RequestMdcFilter;
import com.book.core.memory.application.command.UserMemoryExtraction;
import com.book.core.memory.application.port.AiMemoryExtractionClient;
import com.book.core.memory.application.port.AiMemoryExtractionRequest;
import com.book.core.memory.domain.UserMemoryType;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class AiMemoryExtractionClientImpl implements AiMemoryExtractionClient {
    private static final String EXTRACTION_PATH = "/preferences/extractions";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String SUCCESS_MESSAGE = "extract_success";
    private static final long DEFAULT_RETRY_AFTER_SECONDS = 1;

    private final RestClient restClient;

    public AiMemoryExtractionClientImpl(@Qualifier(AiRestClientConfig.RECOMMENDATION_AI_REST_CLIENT) final RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<UserMemoryExtraction> extract(final AiMemoryExtractionRequest request) {
        final AiExtractionEnvelope envelope;
        try {
            envelope =
                restClient.post().uri(EXTRACTION_PATH).header(REQUEST_ID_HEADER, currentRequestId()).contentType(MediaType.APPLICATION_JSON)
                    .body(new AiExtractionRequest(request.userId(), request.consented(), request.conversation(), request.conversationId()))
                    .retrieve()
                    .onStatus((final HttpStatusCode status) -> status.value() == 429, (final var httpRequest, final var response) -> {
                        throw new RetryAfterException(ErrorCode.AI_MEMORY_EXTRACTION_RATE_LIMITED,
                            retryAfterSeconds(response.getHeaders()));
                    }).onStatus((final HttpStatusCode status) -> status.value() == 503, (final var httpRequest, final var response) -> {
                        throw new CoreException(ErrorCode.AI_MEMORY_EXTRACTION_UNAVAILABLE);
                    }).onStatus((final HttpStatusCode status) -> status.value() == 504, (final var httpRequest, final var response) -> {
                        throw new CoreException(ErrorCode.AI_MEMORY_EXTRACTION_TIMEOUT);
                    }).body(AiExtractionEnvelope.class);
        } catch (final RestClientException exception) {
            if (exception.getCause() instanceof HttpMessageNotReadableException) {
                throw new CoreException(ErrorCode.AI_MEMORY_EXTRACTION_INVALID_RESPONSE, exception);
            }
            throw new CoreException(ErrorCode.AI_SERVICE_FAILURE, exception);
        }
        return toExtractions(envelope, request.conversationId());
    }

    private List<UserMemoryExtraction> toExtractions(final AiExtractionEnvelope envelope, final String conversationId) {
        if (envelope == null || !SUCCESS_MESSAGE.equals(envelope.message()) || envelope.data() == null
            || envelope.data().extractions() == null || envelope.data().nothingFound() == null) {
            throw invalidResponse();
        }
        final List<AiExtraction> extractions = envelope.data().extractions();
        if (envelope.data().nothingFound() && !extractions.isEmpty()) {
            throw invalidResponse();
        }
        return extractions.stream().map((final AiExtraction extraction) -> toExtraction(extraction, conversationId)).toList();
    }

    private UserMemoryExtraction toExtraction(final AiExtraction extraction, final String conversationId) {
        if (extraction == null || Stream.of(extraction.value(), extraction.strength(), extraction.appliesTo(), extraction.keyword(),
            extraction.vector(), extraction.dim()).anyMatch(Objects::isNull) || !conversationId.equals(extraction.sourceConversationId())) {
            throw invalidResponse();
        }
        return new UserMemoryExtraction(toType(extraction.type()), extraction.value(), extraction.strength(), extraction.appliesTo(),
            extraction.keyword(), extraction.vector(), extraction.dim());
    }

    private UserMemoryType toType(final String type) {
        return Arrays.stream(UserMemoryType.values())
            .filter((final UserMemoryType value) -> value.name().toLowerCase(Locale.ROOT).equals(type)).findFirst()
            .orElseThrow(this::invalidResponse);
    }

    private long retryAfterSeconds(final HttpHeaders headers) {
        final String value = headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (value == null) {
            return DEFAULT_RETRY_AFTER_SECONDS;
        }
        try {
            return Long.parseLong(value);
        } catch (final NumberFormatException exception) {
            return DEFAULT_RETRY_AFTER_SECONDS;
        }
    }

    private CoreException invalidResponse() {
        return new CoreException(ErrorCode.AI_MEMORY_EXTRACTION_INVALID_RESPONSE);
    }

    private String currentRequestId() {
        final String requestId = MDC.get(RequestMdcFilter.REQUEST_ID_MDC_KEY);
        if (requestId == null) {
            return UUID.randomUUID().toString();
        }
        return requestId;
    }
}
