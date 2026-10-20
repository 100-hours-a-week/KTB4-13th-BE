package com.book.core.recommendation.infrastructure.client.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RecommendationFeedRefreshClientImplTest {
    private static final String BASE_URL = "http://ai.internal:8000";
    private static final String REFRESH_URL = BASE_URL + "/recommendations/feed/refresh";

    private final RestClient.Builder builder =
        RestClient.builder().baseUrl(BASE_URL).defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer service-token");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final RecommendationFeedRefreshClientImpl client = new RecommendationFeedRefreshClientImpl(builder.build());

    @Test
    void 서비스_토큰과_빈_본문으로_POST하고_남은_목록_수를_반환한다() {
        server.expect(requestTo(REFRESH_URL)).andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer service-token")).andExpect(content().json("{}")).andRespond(withSuccess("""
                {"message": "refresh_success", "data": {"refreshed": 12, "remaining": 30}}
                """, MediaType.APPLICATION_JSON));

        assertThat(client.refreshFeed()).isEqualTo(30);

        server.verify();
    }

    @Test
    void 서버_오류나_잘못된_응답은_실패로_시간_초과는_시간_초과로_구분한다() {
        server.expect(requestTo(REFRESH_URL)).andRespond(withServerError());
        server.expect(requestTo(REFRESH_URL)).andRespond(withSuccess("""
            {"message": "refresh_success", "data": {"refreshed": 0}}
            """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(REFRESH_URL)).andRespond(withException(new SocketTimeoutException("timeout")));

        assertError(ErrorCode.AI_FEED_FAILURE);
        assertError(ErrorCode.AI_FEED_FAILURE);
        assertError(ErrorCode.AI_FEED_TIMEOUT);
    }

    private void assertError(final ErrorCode errorCode) {
        assertThatThrownBy(client::refreshFeed).isInstanceOfSatisfying(CoreException.class,
            (final CoreException exception) -> assertThat(exception.errorCode()).isEqualTo(errorCode));
    }
}
