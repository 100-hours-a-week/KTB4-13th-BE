package com.book.core.recommendation.infrastructure.client.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.recommendation.application.port.RecommendationFeedRequest;
import com.book.core.recommendation.application.port.RecommendationFeedResult;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.web.client.RestClient;

class RecommendationFeedClientImplTest {
    private static final String SERVICE_TOKEN = "super-secret-feed-token";
    private static final String BASE_URL = "http://ai.internal:8000";

    @Test
    void Authorization_Bearer_header를_포함한다() {
        final RestClient.Builder builder = restClientBuilder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final RecommendationFeedClientImpl client = new RecommendationFeedClientImpl(builder.build());

        server.expect(MockRestRequestMatchers.requestTo(BASE_URL + "/recommendations/feed?user_id=42&surface=home&size=15"))
            .andExpect(method(HttpMethod.GET)).andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + SERVICE_TOKEN))
            .andRespond(withSuccess(successBody(), MediaType.APPLICATION_JSON));

        client.getFeed(new RecommendationFeedRequest(42L, 15, null));

        server.verify();
    }

    @Test
    void surface_home에서는_cursor가_없으면_전달하지_않는다() {
        final RestClient.Builder builder = restClientBuilder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final RecommendationFeedClientImpl client = new RecommendationFeedClientImpl(builder.build());

        server.expect(MockRestRequestMatchers.requestTo(BASE_URL + "/recommendations/feed?user_id=42&surface=home&size=15"))
            .andRespond(withSuccess(successBody(), MediaType.APPLICATION_JSON));

        client.getFeed(new RecommendationFeedRequest(42L, 15, null));

        server.verify();
    }

    @Test
    void cursor가_있으면_그대로_전달한다() {
        final RestClient.Builder builder = restClientBuilder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final RecommendationFeedClientImpl client = new RecommendationFeedClientImpl(builder.build());

        server.expect(MockRestRequestMatchers.requestTo(BASE_URL + "/recommendations/feed?user_id=42&surface=home&size=15&cursor=abc"))
            .andExpect(queryParam("cursor", "abc")).andRespond(withSuccess(successBody(), MediaType.APPLICATION_JSON));

        client.getFeed(new RecommendationFeedRequest(42L, 15, "abc"));

        server.verify();
    }

    @Test
    void 정상_응답의_items_nextCursor_coldStart를_매핑한다() {
        final RestClient.Builder builder = restClientBuilder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final RecommendationFeedClientImpl client = new RecommendationFeedClientImpl(builder.build());

        server.expect(MockRestRequestMatchers.requestTo(BASE_URL + "/recommendations/feed?user_id=42&surface=home&size=15"))
            .andRespond(withSuccess(successBody(), MediaType.APPLICATION_JSON));

        final RecommendationFeedResult result = client.getFeed(new RecommendationFeedRequest(42L, 15, null));

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).bookId()).isEqualTo(3310L);
        assertThat(result.items().get(0).title()).isEqualTo("아무튼, 산");
        assertThat(result.items().get(0).inStock()).isTrue();
        assertThat(result.items().get(0).matchScore()).isEqualTo(84);
        assertThat(result.nextCursor()).isEqualTo("next-page");
        assertThat(result.coldStart()).isFalse();
        assertThat(result.degraded()).isNull();
    }

    @Test
    void 빈_목록도_정상_응답으로_처리한다() {
        final RestClient.Builder builder = restClientBuilder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final RecommendationFeedClientImpl client = new RecommendationFeedClientImpl(builder.build());

        server.expect(MockRestRequestMatchers.requestTo(BASE_URL + "/recommendations/feed?user_id=42&surface=home&size=15"))
            .andRespond(withSuccess("{\"message\":\"feed_success\",\"data\":{\"items\":[],\"next_cursor\":null,\"cold_start\":false}}",
                MediaType.APPLICATION_JSON));

        final RecommendationFeedResult result = client.getFeed(new RecommendationFeedRequest(42L, 15, null));

        assertThat(result.items()).isEmpty();
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    void cold_start가_true여도_정상_응답으로_처리한다() {
        final RestClient.Builder builder = restClientBuilder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final RecommendationFeedClientImpl client = new RecommendationFeedClientImpl(builder.build());

        server.expect(MockRestRequestMatchers.requestTo(BASE_URL + "/recommendations/feed?user_id=42&surface=home&size=15"))
            .andRespond(withSuccess("{\"message\":\"feed_success\",\"data\":{\"items\":[],\"next_cursor\":null,\"cold_start\":true}}",
                MediaType.APPLICATION_JSON));

        final RecommendationFeedResult result = client.getFeed(new RecommendationFeedRequest(42L, 15, null));

        assertThat(result.coldStart()).isTrue();
    }

    @Test
    void X_Degraded_헤더가_있으면_degraded로_전달한다() {
        final RestClient.Builder builder = restClientBuilder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final RecommendationFeedClientImpl client = new RecommendationFeedClientImpl(builder.build());

        server.expect(MockRestRequestMatchers.requestTo(BASE_URL + "/recommendations/feed?user_id=42&surface=home&size=15"))
            .andRespond(withSuccess(successBody(), MediaType.APPLICATION_JSON).header("X-Degraded", "rule-only"));

        final RecommendationFeedResult result = client.getFeed(new RecommendationFeedRequest(42L, 15, null));

        assertThat(result.degraded()).isEqualTo("rule-only");
    }

    @Test
    void 상태코드_400은_AI_FEED_INVALID_REQUEST로_변환한다() {
        assertErrorMapping(HttpStatus.BAD_REQUEST, ErrorCode.AI_FEED_INVALID_REQUEST);
    }

    @Test
    void 상태코드_401은_AI_SERVICE_UNAUTHORIZED로_변환한다() {
        assertErrorMapping(HttpStatus.UNAUTHORIZED, ErrorCode.AI_SERVICE_UNAUTHORIZED);
    }

    @Test
    void 상태코드_410은_AI_FEED_CURSOR_EXPIRED로_변환한다() {
        assertErrorMapping(HttpStatus.GONE, ErrorCode.AI_FEED_CURSOR_EXPIRED);
    }

    @Test
    void 상태코드_500은_AI_FEED_FAILURE로_변환한다() {
        assertErrorMapping(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.AI_FEED_FAILURE);
    }

    @Test
    void 연결_실패는_AI_FEED_TIMEOUT으로_변환한다() {
        final RestClient.Builder builder =
            RestClient.builder().baseUrl("http://127.0.0.1:1").defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + SERVICE_TOKEN);
        final RecommendationFeedClientImpl client = new RecommendationFeedClientImpl(builder.build());

        assertThatThrownBy(() -> client.getFeed(new RecommendationFeedRequest(42L, 15, null))).isInstanceOf(CoreException.class)
            .satisfies(exception -> assertThat(((CoreException) exception).errorCode()).isEqualTo(ErrorCode.AI_FEED_TIMEOUT));
    }

    @Test
    void 예외_메시지에_service_token_원문을_노출하지_않는다() {
        final RestClient.Builder builder = restClientBuilder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final RecommendationFeedClientImpl client = new RecommendationFeedClientImpl(builder.build());

        server.expect(MockRestRequestMatchers.requestTo(BASE_URL + "/recommendations/feed?user_id=42&surface=home&size=15"))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.getFeed(new RecommendationFeedRequest(42L, 15, null))).hasMessageNotContaining(SERVICE_TOKEN);
    }

    private void assertErrorMapping(final HttpStatus status, final ErrorCode expected) {
        final RestClient.Builder builder = restClientBuilder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final RecommendationFeedClientImpl client = new RecommendationFeedClientImpl(builder.build());

        server.expect(MockRestRequestMatchers.requestTo(BASE_URL + "/recommendations/feed?user_id=42&surface=home&size=15"))
            .andRespond(withStatus(status));

        assertThatThrownBy(() -> client.getFeed(new RecommendationFeedRequest(42L, 15, null))).isInstanceOf(CoreException.class)
            .satisfies(exception -> assertThat(((CoreException) exception).errorCode()).isEqualTo(expected));

        server.verify();
    }

    private RestClient.Builder restClientBuilder() {
        return RestClient.builder().baseUrl(BASE_URL).defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + SERVICE_TOKEN);
    }

    private String successBody() {
        return """
            {
              "message": "feed_success",
              "data": {
                "items": [
                  {
                    "book_id": 3310,
                    "title": "아무튼, 산",
                    "author": "장보영",
                    "price": 9900,
                    "cover_url": "https://example.com/1.jpg",
                    "in_stock": true,
                    "match_score": 84
                  }
                ],
                "next_cursor": "next-page",
                "cold_start": false
              }
            }
            """;
    }
}
