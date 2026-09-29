package com.book.core.recommendation.infrastructure.client.ai;

import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.time.Duration;
import java.util.function.Predicate;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

class RestClientConfigTest {
    private static final String BASE_URL = "http://ai.internal:8000";
    private static final String SERVICE_TOKEN = "super-secret-ai-token";

    @Test
    void Spring_context가_추천_RestClient를_생성한다() {
        new ApplicationContextRunner().withUserConfiguration(RestClientConfig.class)
            .withPropertyValues("ai.base-url=" + BASE_URL, "ai.service-token=" + SERVICE_TOKEN).run(context -> {
                Assertions.assertThat(context).hasNotFailed().hasSingleBean(RestClient.class);
            });
    }

    @Test
    void AI_URL과_service_token_및_timeout을_추천_RestClient에_적용한다() {
        final RestClient.Builder builder = mock(RestClient.Builder.class, RETURNS_SELF);

        try (final MockedStatic<RestClient> restClients = mockStatic(RestClient.class);
            final MockedConstruction<SimpleClientHttpRequestFactory> requestFactories =
                mockConstruction(SimpleClientHttpRequestFactory.class)) {
            restClients.when(RestClient::builder).thenReturn(builder);
            new RestClientConfig().restClient(new AiServerProperties(BASE_URL, SERVICE_TOKEN));

            verify(builder).baseUrl(BASE_URL);
            verify(builder).defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + SERVICE_TOKEN);
            final SimpleClientHttpRequestFactory requestFactory = requestFactories.constructed().getFirst();
            verify(builder).requestFactory(requestFactory);
            verify(requestFactory).setConnectTimeout(Duration.ofSeconds(3));
            verify(requestFactory).setReadTimeout(Duration.ofSeconds(30));
            final ArgumentCaptor<Predicate<HttpStatusCode>> unauthorizedStatus = ArgumentCaptor.captor();
            final ArgumentCaptor<RestClient.ResponseSpec.ErrorHandler> unauthorizedHandler = ArgumentCaptor.captor();
            verify(builder).defaultStatusHandler(unauthorizedStatus.capture(), unauthorizedHandler.capture());
            Assertions.assertThat(unauthorizedStatus.getValue().test(HttpStatus.UNAUTHORIZED)).isTrue();
            Assertions.assertThat(unauthorizedStatus.getValue().test(HttpStatus.BAD_REQUEST)).isFalse();
            Assertions
                .assertThatThrownBy(() -> unauthorizedHandler.getValue().handle(mock(HttpRequest.class), mock(ClientHttpResponse.class)))
                .isInstanceOfSatisfying(CoreException.class,
                    exception -> Assertions.assertThat(exception.errorCode()).isEqualTo(ErrorCode.AI_SERVICE_UNAUTHORIZED));
            verify(builder).build();
        }
    }
}
