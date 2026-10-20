package com.book.core.recommendation.infrastructure.client.ai;

import com.book.common.config.ai.AiRestClientConfig;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.common.logging.TraceIdFilter;
import com.book.core.recommendation.application.port.RecommendationFeedRefreshClient;
import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class RecommendationFeedRefreshClientImpl implements RecommendationFeedRefreshClient {
    private static final String REFRESH_PATH = "/recommendations/feed/refresh";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String SUCCESS_MESSAGE = "refresh_success";

    private final RestClient restClient;

    public RecommendationFeedRefreshClientImpl(@Qualifier(AiRestClientConfig.RECOMMENDATION_AI_REST_CLIENT) final RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public int refreshFeed() {
        final AiFeedRefreshEnvelope envelope;
        try {
            envelope = restClient.post().uri(REFRESH_PATH).header(REQUEST_ID_HEADER, currentRequestId())
                .contentType(MediaType.APPLICATION_JSON).body(Map.of()).retrieve().body(AiFeedRefreshEnvelope.class);
        } catch (final ResourceAccessException exception) {
            throw new CoreException(ErrorCode.AI_FEED_TIMEOUT, exception);
        } catch (final RestClientException exception) {
            throw new CoreException(ErrorCode.AI_FEED_FAILURE, exception);
        }
        if (envelope == null || !SUCCESS_MESSAGE.equals(envelope.message()) || envelope.data() == null
            || envelope.data().remaining() == null || envelope.data().remaining() < 0) {
            throw new CoreException(ErrorCode.AI_FEED_FAILURE);
        }
        return envelope.data().remaining();
    }

    private String currentRequestId() {
        final String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
        return traceId != null ? traceId : UUID.randomUUID().toString();
    }
}
