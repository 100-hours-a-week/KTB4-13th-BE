package com.book.core.recommendation.infrastructure.client.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.book.common.config.ai.AiChatHttpClient;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.common.exception.RetryAfterException;
import com.book.common.filter.RequestMdcFilter;
import com.book.core.book.application.usecase.GetAiBookIdentifiersUseCase;
import com.book.core.onboarding.application.result.OwnedBook;
import com.book.core.onboarding.application.result.RecommendationHistory;
import com.book.core.onboarding.application.usecase.GetRecommendationHistoryUseCase;
import com.book.core.recommendation.application.command.RecommendationTurn;
import com.book.core.recommendation.application.port.AiRecommendationChatRequest;
import com.book.core.recommendation.application.port.AiRecommendationChatResult;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class AiRecommendationClientImplTest {
    private static final String SERVICE_TOKEN = "super-secret-recommendation-token";
    private static final String BASE_URL = "http://ai.internal:8000";

    private final GetAiBookIdentifiersUseCase identifiers = mock(GetAiBookIdentifiersUseCase.class);
    private final GetRecommendationHistoryUseCase history = mock(GetRecommendationHistoryUseCase.class);

    @BeforeEach
    void resolveIsbns() {
        when(history.execute(42L)).thenReturn(new RecommendationHistory(true, List.of()));
        when(identifiers.findBookIds(org.mockito.ArgumentMatchers.anyList())).thenReturn(Map.of("9788900000011", 1L));
        when(identifiers.findIsbns(List.of(9L))).thenReturn(List.of("9788900000059"));
    }

    private AiRecommendationClientImpl client(final RestClient.Builder builder) {
        final AiChatHttpClient http = mock(AiChatHttpClient.class);
        when(http.before(anyLong())).thenReturn(builder.build());
        return new AiRecommendationClientImpl(http, identifiers, history, new ObjectMapper());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"data\":{\"spec\":{},\"reply\":\"draft\",\"cards\":[],\"buttons\":[]}}",
        "{\"data\":{\"spec\":{},\"reply\":\"draft\",\"buttons\":[],\"degraded\":false}}"})
    void 필수_필드가_빠진_최종_응답은_JSON과_SSE_모두_AI_실패로_처리한다(final String body) {
        final RestClient.Builder builder = restClientBuilder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final AiRecommendationClientImpl client = client(builder);
        server.expect(requestTo(BASE_URL + "/recommendations/chat")).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/recommendations/chat"))
            .andRespond(withSuccess("event: done\ndata: " + body + "\n\n", MediaType.TEXT_EVENT_STREAM));
        final AiRecommendationChatRequest request = new AiRecommendationChatRequest(42L, true, Map.of(), "추천", List.of(), List.of());

        assertThatThrownBy(() -> client.chat(request)).isInstanceOfSatisfying(CoreException.class,
            failure -> assertThat(failure.errorCode()).isEqualTo(ErrorCode.AI_RECOMMENDATION_FAILURE));
        assertThatThrownBy(() -> client.chat(request, text -> {
        })).isInstanceOfSatisfying(CoreException.class,
            failure -> assertThat(failure.errorCode()).isEqualTo(ErrorCode.AI_RECOMMENDATION_FAILURE));
        server.verify();
    }

    @Test
    void 전송_본문이_64KiB를_넘으면_최근_이력만_실제로_보낸다() {
        final LocalDateTime date = LocalDateTime.of(2026, 10, 21, 0, 0);
        final List<OwnedBook> books = IntStream.range(0, 5000)
            .mapToObj((final int index) -> new OwnedBook(Long.toString(9_780_000_000_000L + index), date.minusMinutes(index), true))
            .toList();
        when(history.execute(42L)).thenReturn(new RecommendationHistory(true, books));
        final RestClient.Builder builder = restClientBuilder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final AiRecommendationClientImpl client = client(builder);
        server.expect(requestTo(BASE_URL + "/recommendations/chat"))
            .andExpect((final ClientHttpRequest request) -> assertThat(((MockClientHttpRequest) request).getBodyAsBytes().length)
                .isBetween(65_521, 65_536))
            .andExpect(jsonPath("$.consented").value(true)).andExpect(jsonPath("$.purchased_isbns[0]").value("9780000000000"))
            .andExpect(jsonPath("$.purchased_isbns[4999]").doesNotExist()).andExpect(jsonPath("$.library_isbns").isEmpty())
            .andRespond(withSuccess(successBody(), MediaType.APPLICATION_JSON));

        client.chat(new AiRecommendationChatRequest(42L, false, Map.of(), "추천해줘", List.of(), List.of()));

        server.verify();
    }

    @Test
    void 요청_플래그가_true여도_저장된_동의가_없으면_빈_목록을_명시한다() {
        when(history.execute(42L)).thenReturn(RecommendationHistory.NOT_CONSENTED);
        final RestClient.Builder builder = restClientBuilder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final AiRecommendationClientImpl client = client(builder);
        server.expect(requestTo(BASE_URL + "/recommendations/chat")).andExpect(jsonPath("$.purchased_isbns").isEmpty())
            .andExpect(jsonPath("$.library_isbns").isEmpty()).andExpect(jsonPath("$.consented").value(false))
            .andRespond(withSuccess(successBody(), MediaType.APPLICATION_JSON));

        client.chat(new AiRecommendationChatRequest(42L, true, Map.of(), "추천해줘", List.of(), List.of()));

        server.verify();
    }

    @Test
    void SSE_조각을_전달하고_완료_이벤트를_기존_추천_응답으로_변환한다() {
        final RestClient.Builder builder = restClientBuilder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final AiRecommendationClientImpl client = client(builder);
        server.expect(requestTo(BASE_URL + "/recommendations/chat")).andExpect(header(HttpHeaders.ACCEPT, "text/event-stream"))
            .andRespond(withSuccess(
                "event: delta\r\ndata: {\"text\":\"먼저\"}\r\n\r\nevent: done\ndata: " + successBody().strip().replace("\n", "") + "\n\n",
                MediaType.TEXT_EVENT_STREAM));
        final List<String> deltas = new ArrayList<>();

        final AiRecommendationChatResult result = client.chat(chatRequest(), deltas::add);

        assertThat(deltas).containsExactly("먼저");
        assertThat(result.cards()).hasSize(1);
        server.verify();
    }

    @Test
    void SSE_오류_메시지를_기존_타임아웃_코드로_변환한다() {
        final RestClient.Builder builder = restClientBuilder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final AiRecommendationClientImpl client = client(builder);
        server.expect(requestTo(BASE_URL + "/recommendations/chat")).andRespond(withSuccess("""
            event: error
            data: {"message":"generation_timeout","data":null}

            """, MediaType.TEXT_EVENT_STREAM));

        assertThatThrownBy(() -> client.chat(chatRequest(), (final String text) -> {
        })).isInstanceOfSatisfying(CoreException.class,
            (final CoreException failure) -> assertThat(failure.errorCode()).isEqualTo(ErrorCode.AI_RECOMMENDATION_TIMEOUT));
        server.verify();
    }

    @AfterEach
    void clearMdc() {
        MDC.remove(RequestMdcFilter.REQUEST_ID_MDC_KEY);
    }

    @Test
    void Authorization_Bearer와_X_Request_Id를_요청_header에_포함한다() {
        MDC.put(RequestMdcFilter.REQUEST_ID_MDC_KEY, "trace-abc-123");
        final RestClient.Builder builder = restClientBuilder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final AiRecommendationClientImpl client = client(builder);

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
        final AiRecommendationClientImpl client = client(builder);

        server.expect(requestTo(BASE_URL + "/recommendations/chat")).andExpect(jsonPath("$.user_id").value(42))
            .andExpect(jsonPath("$.consented").value(true)).andExpect(jsonPath("$.message").value("추천해줘"))
            .andExpect(jsonPath("$.recent_turns[0].text").value("이전 대화")).andExpect(jsonPath("$.exclude_isbns[0]").value("9788900000059"))
            .andRespond(withSuccess(successBody(), MediaType.APPLICATION_JSON));

        client.chat(chatRequest());

        server.verify();
    }

    @Test
    void AI가_null인_긴_이유를_보내도_카드를_버리지_않는다() {
        final RestClient.Builder builder = restClientBuilder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final AiRecommendationClientImpl client = client(builder);
        final String body = successBody().replace("\"reason_long\": \"긴 추천 이유\"", "\"reason_long\": null");
        assertThat(body).contains("\"reason_long\": null");
        server.expect(requestTo(BASE_URL + "/recommendations/chat")).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        final AiRecommendationChatResult result = client.chat(chatRequest());

        assertThat(result.cards()).hasSize(1);
        assertThat(result.cards().getFirst().reasonLong()).isNull();
        server.verify();
    }

    @Test
    void 정상_응답을_결과로_매핑한다() {
        final RestClient.Builder builder = restClientBuilder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final AiRecommendationClientImpl client = client(builder);

        server.expect(requestTo(BASE_URL + "/recommendations/chat")).andRespond(withSuccess(successBody(), MediaType.APPLICATION_JSON));

        final AiRecommendationChatResult result = client.chat(chatRequest());

        assertThat(result.reply()).isEqualTo("이 책들을 추천합니다");
        assertThat(result.cards()).hasSize(1);
        assertThat(result.cards().getFirst().bookId()).isEqualTo(1L);
        assertThat(result.cards().getFirst().matchScore()).isEqualTo(87);
        assertThat(result.cards().getFirst().reasonShort()).isEqualTo("한 줄 추천 이유");
        assertThat(result.cards().getFirst().reasonLong()).isEqualTo("긴 추천 이유");
        assertThat(result.degraded()).isFalse();

        server.verify();
    }

    @Test
    void spec_schema_violation_422는_초기_spec으로_한_번만_재시도한다() {
        final RestClient.Builder builder = restClientBuilder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final AiRecommendationClientImpl client = client(builder);

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
        final AiRecommendationClientImpl client = client(builder);

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
        final AiRecommendationClientImpl client = client(builder);

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
        final AiRecommendationClientImpl client = client(builder);

        server.expect(requestTo(BASE_URL + "/recommendations/chat")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.chat(chatRequest())).isInstanceOf(CoreException.class)
            .satisfies(exception -> assertThat(((CoreException) exception).errorCode()).isEqualTo(ErrorCode.AI_SERVICE_UNAUTHORIZED));

        server.verify();
    }

    @Test
    void 상태코드_429는_재시도하지_않고_Retry_After를_전달한다() {
        final RestClient.Builder builder = restClientBuilder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final AiRecommendationClientImpl client = client(builder);

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
        final AiRecommendationClientImpl client = client(builder);

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
        final AiRecommendationClientImpl client = client(builder);

        server.expect(requestTo(BASE_URL + "/recommendations/chat")).andRespond(withStatus(HttpStatus.GATEWAY_TIMEOUT));

        assertThatThrownBy(() -> client.chat(chatRequest())).isInstanceOf(CoreException.class)
            .satisfies(exception -> assertThat(((CoreException) exception).errorCode()).isEqualTo(ErrorCode.AI_RECOMMENDATION_TIMEOUT));

        server.verify();
    }

    @Test
    void 예외_메시지에_service_token_원문을_노출하지_않는다() {
        final RestClient.Builder builder = restClientBuilder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final AiRecommendationClientImpl client = client(builder);

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
                "cards": [{"isbn": "9788900000011", "rank": 1, "match_score": 87, "title": "제목", "price": 13500,
                  "reason_short": "한 줄 추천 이유", "reason_long": "긴 추천 이유", "match_basis": []}],
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
