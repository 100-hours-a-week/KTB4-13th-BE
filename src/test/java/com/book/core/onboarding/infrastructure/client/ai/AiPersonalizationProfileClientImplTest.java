package com.book.core.onboarding.infrastructure.client.ai;

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
import com.book.common.logging.TraceIdFilter;
import com.book.core.onboarding.application.port.AiPersonalizationProfileRequest;
import java.net.SocketTimeoutException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AiPersonalizationProfileClientImplTest {
    private static final String SERVICE_TOKEN = "profile-service-token";
    private static final String BASE_URL = "http://ai.internal:8000";
    private static final String PROFILE_URL = BASE_URL + "/preferences/profile";

    private final RestClient.Builder builder =
        RestClient.builder().baseUrl(BASE_URL).defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + SERVICE_TOKEN);
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final AiPersonalizationProfileClientImpl client = new AiPersonalizationProfileClientImpl(builder.build());

    @AfterEach
    void clearMdc() {
        MDC.remove(TraceIdFilter.TRACE_ID_MDC_KEY);
    }

    @Test
    void POST_preferences_profile에_Bearer_토큰과_X_Request_Id를_담아_요청한다() {
        MDC.put(TraceIdFilter.TRACE_ID_MDC_KEY, "trace-abc-123");
        server.expect(requestTo(PROFILE_URL)).andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + SERVICE_TOKEN)).andExpect(header("X-Request-Id", "trace-abc-123"))
            .andRespond(withSuccess());

        client.createProfile(profileRequest());

        server.verify();
    }

    @Test
    void 요청_본문을_AI_계약의_snake_case로_보낸다() {
        final AiPersonalizationProfileRequest request = profileRequest();
        server.expect(requestTo(PROFILE_URL)).andExpect(jsonPath("$.user_id").value(42))
            .andExpect(jsonPath("$.idempotency_key").value(request.idempotencyKey()))
            .andExpect(jsonPath("$.onboarding.reading_times[0]").value("잠들기 전"))
            .andExpect(jsonPath("$.onboarding.criteria[0]").value("베스트셀러")).andExpect(jsonPath("$.onboarding.categories[0]").value("소설"))
            .andExpect(jsonPath("$.onboarding.tags[0]").value("SF")).andExpect(jsonPath("$.onboarding.liked_book_ids[1]").value(300))
            .andExpect(jsonPath("$.memories").isEmpty()).andRespond(withSuccess());

        client.createProfile(request);

        server.verify();
    }

    @Test
    void AI가_오류_상태를_응답하면_공통_AI_실패로_변환한다() {
        server.expect(requestTo(PROFILE_URL)).andRespond(withStatus(HttpStatus.CONFLICT));

        assertThatThrownBy(() -> client.createProfile(profileRequest())).isInstanceOf(CoreException.class)
            .satisfies(exception -> assertThat(((CoreException) exception).errorCode()).isEqualTo(ErrorCode.AI_RECOMMENDATION_FAILURE));
    }

    @Test
    void 응답_시간_초과나_연결_실패는_공통_AI_실패로_변환한다() {
        server.expect(requestTo(PROFILE_URL)).andRespond(withException(new SocketTimeoutException("read timed out")));

        assertThatThrownBy(() -> client.createProfile(profileRequest())).isInstanceOf(CoreException.class)
            .satisfies(exception -> assertThat(((CoreException) exception).errorCode()).isEqualTo(ErrorCode.AI_RECOMMENDATION_FAILURE));
    }

    private static AiPersonalizationProfileRequest profileRequest() {
        return new AiPersonalizationProfileRequest(42L, List.of("잠들기 전"), List.of("베스트셀러"), List.of("소설"), List.of("SF"),
            List.of(100L, 300L));
    }
}
