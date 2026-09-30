package com.book.core.recommendation.infrastructure.client.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.common.exception.RetryAfterException;
import com.book.common.logging.TraceIdFilter;
import com.book.core.recommendation.application.command.RecommendationTurn;
import com.book.core.recommendation.application.port.AiRecommendationChatRequest;
import com.book.core.recommendation.application.port.AiRecommendationChatResult;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AiRecommendationClientImplTest {
    private static final String SERVICE_TOKEN = "super-secret-recommendation-token";
    private static final String BASE_URL = "http://ai.internal:8000";

    @AfterEach
    void clearMdc() {
        MDC.remove(TraceIdFilter.TRACE_ID_MDC_KEY);
    }

    @Test
    void Authorization_Bearer와_X_Request_Id를_요청_header에_포함한다() {
        MDC.put(TraceIdFilter.TRACE_ID_MDC_KEY, "trace-abc-123");
        final RestClient.Builder builder = restClientBuilder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final AiRecommendationClientImpl client = new AiRecommendationClientImpl(builder.build());

        server.expect(requestTo(BASE_URL + "/recommendations/chat")).andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + SERVICE_TOKEN)).andExpect(header("X-Request-Id", "trace-abc-123"))
            .andRespond(withSuccess(successBody(), MediaType.APPLICATION_JSON));

        client.chat(chatRequest());

        server.verify();
    }

    @Test
    void 요청_필드를_AI_계약의_snake_case로_매핑한다() {
        final RestClient.Builder builder = restClientBuilder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final AiRecommendationClientImpl client = new AiRecommendationClientImpl(builder.build());

        server.expect(requestTo(BASE_URL + "/recommendations/chat")).andExpect(jsonPath("$.user_id").value(42))
            .andExpect(jsonPath("$.consented").value(true)).andExpect(jsonPath("$.message").value("추천해줘"))
            .andExpect(jsonPath("$.recent_turns[0].text").value("이전 대화")).andExpect(jsonPath("$.exclude_book_ids[0]").value(9))
            .andRespond(withSuccess(successBody(), MediaType.APPLICATION_JSON));

        client.chat(chatRequest());

        server.verify();
    }

    @Test
    void 정상_응답을_결과로_매핑한다() {
        final RestClient.Builder builder = restClientBuilder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final AiRecommendationClientImpl client = new AiRecommendationClientImpl(builder.build());

        server.expect(requestTo(BASE_URL + "/recommendations/chat")).andRespond(withSuccess(successBody(), MediaType.APPLICATION_JSON));

        final AiRecommendationChatResult result = client.chat(chatRequest());

        assertThat(result.reply()).isEqualTo("이 책들을 추천합니다");
        assertThat(result.cards()).hasSize(1);
        assertThat(result.cards().getFirst().bookId()).isEqualTo(1L);
        assertThat(result.cards().getFirst().reasonLong()).isEqualTo("긴 추천 이유");
        assertThat(result.degraded()).isFalse();

        server.verify();
    }

    @Test
    void spec_schema_violation_422는_초기_spec으로_한_번만_재시도한다() {
        final RestClient.Builder builder = restClientBuilder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final AiRecommendationClientImpl client = new AiRecommendationClientImpl(builder.build());

        server.expect(requestTo(BASE_URL + "/recommendations/chat")).andExpect(jsonPath("$.spec.intent").value("keyword")).andRespond(
            withStatus(HttpStatus.UNPROCESSABLE_ENTITY).contentType(MediaType.APPLICATION_JSON).body(errorBody("spec_schema_violation")));
        server.expect(requestTo(BASE_URL + "/recommendations/chat")).andExpect(jsonPath("$.spec.intent").value("semantic"))
            .andRespond(withSuccess(successBody(), MediaType.APPLICATION_JSON));

        final AiRecommendationChatResult result = client.chat(chatRequestWithSpec(Map.of("intent", "keyword")));

        assertThat(result.reply()).isEqualTo("이 책들을 추천합니다");
        server.verify();
    }

    @Test
    void 재시도에서도_spec_schema_violation이면_오류로_변환한다() {
        final RestClient.Builder builder = restClientBuilder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final AiRecommendationClientImpl client = new AiRecommendationClientImpl(builder.build());

        server.expect(requestTo(BASE_URL + "/recommendations/chat")).andRespond(
            withStatus(HttpStatus.UNPROCESSABLE_ENTITY).contentType(MediaType.APPLICATION_JSON).body(errorBody("spec_schema_violation")));
        server.expect(requestTo(BASE_URL + "/recommendations/chat")).andRespond(
            withStatus(HttpStatus.UNPROCESSABLE_ENTITY).contentType(MediaType.APPLICATION_JSON).body(errorBody("spec_schema_violation")));

        assertThatThrownBy(() -> client.chat(chatRequest())).isInstanceOf(CoreException.class).satisfies(
            exception -> assertThat(((CoreException) exception).errorCode()).isEqualTo(ErrorCode.AI_RECOMMENDATION_SPEC_VIOLATION));

        server.verify();
    }

    @Test
    void 다른_422_오류는_초기_spec으로_재시도하지_않고_일반_오류로_변환한다() {
        final AtomicInteger requestCount = new AtomicInteger();
        final RestClient.Builder builder = restClientBuilder().requestInterceptor((request, body, execution) -> {
            requestCount.incrementAndGet();
            return execution.execute(request, body);
        });
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final AiRecommendationClientImpl client = new AiRecommendationClientImpl(builder.build());

        server.expect(requestTo(BASE_URL + "/recommendations/chat")).andRespond(
            withStatus(HttpStatus.UNPROCESSABLE_ENTITY).contentType(MediaType.APPLICATION_JSON).body(errorBody("invalid_request")));

        assertThatThrownBy(() -> client.chat(chatRequest())).isInstanceOf(CoreException.class)
            .satisfies(exception -> assertThat(((CoreException) exception).errorCode()).isEqualTo(ErrorCode.AI_RECOMMENDATION_FAILURE));

        assertThat(requestCount.get()).isEqualTo(1);
        server.verify();
    }

    @Test
    void 상태코드_401은_AI_service_token_오류로_변환한다() {
        final RestClient.Builder builder = restClientBuilder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final AiRecommendationClientImpl client = new AiRecommendationClientImpl(builder.build());

        server.expect(requestTo(BASE_URL + "/recommendations/chat")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.chat(chatRequest())).isInstanceOf(CoreException.class)
            .satisfies(exception -> assertThat(((CoreException) exception).errorCode()).isEqualTo(ErrorCode.AI_SERVICE_UNAUTHORIZED));

        server.verify();
    }

    @Test
    void 상태코드_429는_재시도하지_않고_Retry_After를_전달한다() {
        final RestClient.Builder builder = restClientBuilder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final AiRecommendationClientImpl client = new AiRecommendationClientImpl(builder.build());

        server.expect(requestTo(BASE_URL + "/recommendations/chat"))
            .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header(HttpHeaders.RETRY_AFTER, "20"));

        assertThatThrownBy(() -> client.chat(chatRequest())).isInstanceOf(RetryAfterException.class).satisfies(exception -> {
            final RetryAfterException retryAfterException = (RetryAfterException) exception;
            assertThat(retryAfterException.errorCode()).isEqualTo(ErrorCode.AI_RECOMMENDATION_RATE_LIMITED);
            assertThat(retryAfterException.retryAfterSeconds()).isEqualTo(20);
        });

        server.verify();
    }

    @Test
    void 상태코드_503은_재시도하지_않고_Retry_After를_전달한다() {
        final RestClient.Builder builder = restClientBuilder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final AiRecommendationClientImpl client = new AiRecommendationClientImpl(builder.build());

        server.expect(requestTo(BASE_URL + "/recommendations/chat"))
            .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE).header(HttpHeaders.RETRY_AFTER, "5"));

        assertThatThrownBy(() -> client.chat(chatRequest())).isInstanceOf(RetryAfterException.class).satisfies(exception -> {
            final RetryAfterException retryAfterException = (RetryAfterException) exception;
            assertThat(retryAfterException.errorCode()).isEqualTo(ErrorCode.AI_RECOMMENDATION_UNAVAILABLE);
            assertThat(retryAfterException.retryAfterSeconds()).isEqualTo(5);
        });

        server.verify();
    }

    @Test
    void 상태코드_504는_재시도하지_않는다() {
        final RestClient.Builder builder = restClientBuilder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final AiRecommendationClientImpl client = new AiRecommendationClientImpl(builder.build());

        server.expect(requestTo(BASE_URL + "/recommendations/chat")).andRespond(withStatus(HttpStatus.GATEWAY_TIMEOUT));

        assertThatThrownBy(() -> client.chat(chatRequest())).isInstanceOf(CoreException.class)
            .satisfies(exception -> assertThat(((CoreException) exception).errorCode()).isEqualTo(ErrorCode.AI_RECOMMENDATION_TIMEOUT));

        server.verify();
    }

    @Test
    void 예외_메시지에_service_token_원문을_노출하지_않는다() {
        final RestClient.Builder builder = restClientBuilder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final AiRecommendationClientImpl client = new AiRecommendationClientImpl(builder.build());

        server.expect(requestTo(BASE_URL + "/recommendations/chat")).andRespond(withStatus(HttpStatus.GATEWAY_TIMEOUT));

        assertThatThrownBy(() -> client.chat(chatRequest())).hasMessageNotContaining(SERVICE_TOKEN);
    }

    private RestClient.Builder restClientBuilder() {
        return RestClient.builder().baseUrl(BASE_URL).defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + SERVICE_TOKEN);
    }

    private AiRecommendationChatRequest chatRequest() {
        return chatRequestWithSpec(Map.of("intent", "semantic"));
    }

    private AiRecommendationChatRequest chatRequestWithSpec(final Map<String, Object> spec) {
        return new AiRecommendationChatRequest(42L, true, spec, "추천해줘", List.of(new RecommendationTurn("user", "이전 대화")), List.of(9L));
    }

    private String successBody() {
        return """
            {
              "message": "recommend_success",
              "data": {
                "spec": {"intent": "semantic"},
                "reply": "이 책들을 추천합니다",
                "cards": [{"book_id": 1, "reason_long": "긴 추천 이유"}],
                "followup": "더 알고 싶은 게 있나요?",
                "buttons": ["다시 추천"],
                "degraded": false
              }
            }
            """;
    }

    private String errorBody(final String code) {
        return """
            {"message":"%s","data":null}
            """.formatted(code);
    }
}
