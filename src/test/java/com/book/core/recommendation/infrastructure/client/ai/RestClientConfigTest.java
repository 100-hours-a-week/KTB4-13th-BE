package com.book.core.recommendation.infrastructure.client.ai;

import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

class RestClientConfigTest {
    private static final String BASE_URL = "http://ai.internal:8000";
    private static final String SERVICE_TOKEN = "super-secret-ai-token";

    @Test
    void AI_URL과_service_token_및_timeout을_추천_RestClient에_적용한다() {
        final RestClient.Builder builder = mock(RestClient.Builder.class, RETURNS_SELF);

        try (final MockedConstruction<SimpleClientHttpRequestFactory> requestFactories =
            mockConstruction(SimpleClientHttpRequestFactory.class)) {
            new RestClientConfig().restClient(builder, new AiServerProperties(BASE_URL, SERVICE_TOKEN));

            verify(builder).baseUrl(BASE_URL);
            verify(builder).defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + SERVICE_TOKEN);
            final SimpleClientHttpRequestFactory requestFactory = requestFactories.constructed().getFirst();
            verify(builder).requestFactory(requestFactory);
            verify(requestFactory).setConnectTimeout(Duration.ofSeconds(3));
            verify(requestFactory).setReadTimeout(Duration.ofSeconds(30));
            verify(builder).build();
        }
    }
}
