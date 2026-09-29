package com.book.core.search.infrastructure.client.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.common.exception.RetryAfterException;
import com.book.common.logging.TraceIdFilter;
import com.book.core.search.application.command.BookSearchSort;
import com.book.core.search.application.port.BookSearchItem;
import com.book.core.search.application.port.BookSearchRequest;
import com.book.core.search.application.port.BookSearchResult;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class BookSearchClientImplTest {
    private static final String SERVICE_TOKEN = "search-service-token";
    private static final String BASE_URL = "http://ai.internal:8000";
    private static final String SEARCH_URL = BASE_URL + "/search";

    private final RestClient.Builder builder =
        RestClient.builder().baseUrl(BASE_URL).defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + SERVICE_TOKEN);
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final BookSearchClientImpl client = new BookSearchClientImpl(builder.build());

    @AfterEach
    void clearMdc() {
        MDC.remove(TraceIdFilter.TRACE_ID_MDC_KEY);
    }

    @Test
    void POST_search에_Bearer_토큰과_X_Request_Id와_snake_case_본문을_보낸다() {
        MDC.put(TraceIdFilter.TRACE_ID_MDC_KEY, "trace-abc-123");
        server.expect(requestTo(SEARCH_URL)).andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + SERVICE_TOKEN)).andExpect(header("X-Request-Id", "trace-abc-123"))
            .andExpect(jsonPath("$.query").value("투자 입문")).andExpect(jsonPath("$.filters.category").value("경제경영"))
            .andExpect(jsonPath("$.filters.price_min").value(10000)).andExpect(jsonPath("$.filters.price_max").value(20000))
            .andExpect(jsonPath("$.filters.pub_year_from").value(2020)).andExpect(jsonPath("$.filters.pub_year_to").value(2024))
            .andExpect(jsonPath("$.sort").value("price_asc")).andExpect(jsonPath("$.cursor").value("abc"))
            .andExpect(jsonPath("$.size").value(20)).andRespond(withSuccess(body("[]", null, null), MediaType.APPLICATION_JSON));

        client.search(new BookSearchRequest("투자 입문", "경제경영", 10000, 20000, 2020, 2024, BookSearchSort.PRICE_ASC, "abc", 20));

        server.verify();
    }

    @Test
    void 값이_없는_필터와_cursor는_보내지_않는다() {
        server.expect(requestTo(SEARCH_URL)).andExpect(jsonPath("$.query").value("소설")).andExpect(jsonPath("$.sort").value("popular"))
            .andExpect(jsonPath("$.size").value(12)).andExpect(jsonPath("$.cursor").doesNotExist())
            .andExpect(jsonPath("$.filters.category").doesNotExist()).andExpect(jsonPath("$.filters.price_min").doesNotExist())
            .andExpect(jsonPath("$.filters.pub_year_from").doesNotExist()).andExpect(jsonPath("$.filters.in_stock_only").doesNotExist())
            .andRespond(withSuccess(body("[]", null, null), MediaType.APPLICATION_JSON));

        client.search(new BookSearchRequest("소설", null, null, null, null, null, BookSearchSort.POPULAR, null, 12));

        server.verify();
    }

    @Test
    void 검색_결과와_다음_cursor를_매핑한다() {
        server.expect(requestTo(SEARCH_URL)).andRespond(withSuccess(body("""
            [{"book_id": 2077, "title": "여행의 이유", "author": "김영하", "publisher": "문학동네", "price": 13500,
              "in_stock": true, "cover_url": "https://example.com/2077.jpg"}]""", "\"next-page\"", null), MediaType.APPLICATION_JSON));

        final BookSearchResult result = client.search(popularRequest());

        assertThat(result.items()).containsExactly(
            new BookSearchItem(2077L, "여행의 이유", "김영하", "문학동네", new BigDecimal("13500"), true, "https://example.com/2077.jpg"));
        assertThat(result.nextCursor()).isEqualTo("next-page");
        assertThat(result.fallbackMessage()).isNull();
        assertThat(result.degraded()).isNull();
    }

    @Test
    void 결과가_없으면_fallback_문구와_X_Degraded를_전달한다() {
        server.expect(requestTo(SEARCH_URL))
            .andRespond(withSuccess(body("[]", null, "{\"message\": \"원하는 책을 못 찾았어요. AI 추천에게 물어볼까요?\"}"), MediaType.APPLICATION_JSON)
                .header("X-Degraded", "keyword-only"));

        final BookSearchResult result = client.search(popularRequest());

        assertThat(result.items()).isEmpty();
        assertThat(result.fallbackMessage()).isEqualTo("원하는 책을 못 찾았어요. AI 추천에게 물어볼까요?");
        assertThat(result.degraded()).isEqualTo("keyword-only");
    }

    @Test
    void AI_오류_상태를_공통_오류로_변환한다() {
        assertErrorMapping(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REQUEST);
        assertErrorMapping(HttpStatus.UNAUTHORIZED, ErrorCode.AI_SERVICE_UNAUTHORIZED);
        assertErrorMapping(HttpStatus.GONE, ErrorCode.AI_SEARCH_CURSOR_EXPIRED);
        assertErrorMapping(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.AI_SEARCH_FAILURE);
    }

    @Test
    void AI_429는_Retry_After와_함께_AI_SEARCH_RATE_LIMITED로_변환한다() {
        server.expect(requestTo(SEARCH_URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header(HttpHeaders.RETRY_AFTER, "30"));

        assertThatThrownBy(() -> client.search(popularRequest())).isInstanceOfSatisfying(RetryAfterException.class, exception -> {
            assertThat(exception.errorCode()).isEqualTo(ErrorCode.AI_SEARCH_RATE_LIMITED);
            assertThat(exception.retryAfterSeconds()).isEqualTo(30);
        });
    }

    @Test
    void AI_429에_Retry_After가_없으면_1초로_안내한다() {
        server.expect(requestTo(SEARCH_URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> client.search(popularRequest())).isInstanceOfSatisfying(RetryAfterException.class, exception -> {
            assertThat(exception.errorCode()).isEqualTo(ErrorCode.AI_SEARCH_RATE_LIMITED);
            assertThat(exception.retryAfterSeconds()).isEqualTo(1);
        });
    }

    @Test
    void 응답_시간_초과나_연결_실패는_AI_SEARCH_TIMEOUT으로_변환한다() {
        server.expect(requestTo(SEARCH_URL)).andRespond(withException(new SocketTimeoutException("read timed out")));

        assertThatThrownBy(() -> client.search(popularRequest())).isInstanceOf(CoreException.class)
            .satisfies(exception -> assertThat(((CoreException) exception).errorCode()).isEqualTo(ErrorCode.AI_SEARCH_TIMEOUT));
    }

    private void assertErrorMapping(final HttpStatus status, final ErrorCode expected) {
        server.reset();
        server.expect(requestTo(SEARCH_URL)).andRespond(withStatus(status));

        assertThatThrownBy(() -> client.search(popularRequest())).isInstanceOf(CoreException.class)
            .satisfies(exception -> assertThat(((CoreException) exception).errorCode()).isEqualTo(expected));
    }

    private static BookSearchRequest popularRequest() {
        return new BookSearchRequest("여행", null, null, null, null, null, BookSearchSort.POPULAR, null, 12);
    }

    private static String body(final String results, final String nextCursor, final String fallback) {
        return """
            {"message": "search_success", "data": {"results": %s, "next_cursor": %s, "fallback": %s}}
            """.formatted(results, nextCursor, fallback);
    }
}
