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
import com.book.common.filter.RequestMdcFilter;
import com.book.core.onboarding.application.port.AiPersonalizationProfileHistory;
import com.book.core.onboarding.application.port.AiPersonalizationProfileMemory;
import com.book.core.onboarding.application.port.AiPersonalizationProfileRequest;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
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
    private final PersonalizationProfileIdempotencyKeyGenerator idempotencyKeyGenerator =
        new PersonalizationProfileIdempotencyKeyGenerator();
    private final AiPersonalizationProfileClientImpl client =
        new AiPersonalizationProfileClientImpl(builder.build(), idempotencyKeyGenerator);

    @AfterEach
    void clearMdc() {
        MDC.remove(RequestMdcFilter.REQUEST_ID_MDC_KEY);
    }

    @Test
    void POST_preferences_profile에_Bearer_토큰과_X_Request_Id를_담아_요청한다() {
        MDC.put(RequestMdcFilter.REQUEST_ID_MDC_KEY, "request-abc-123");
        server.expect(requestTo(PROFILE_URL)).andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + SERVICE_TOKEN)).andExpect(header("X-Request-Id", "request-abc-123"))
            .andRespond(withSuccess());

        client.createProfile(profileRequest());

        server.verify();
    }

    @Test
    void 요청_본문을_AI_계약의_snake_case로_보낸다() {
        final AiPersonalizationProfileRequest request = profileRequest();
        server.expect(requestTo(PROFILE_URL)).andExpect(jsonPath("$.user_id").value(42))
            .andExpect(jsonPath("$.idempotency_key").value(idempotencyKeyGenerator.generate(request)))
            .andExpect(jsonPath("$.onboarding.reading_times[0]").value("잠들기 전"))
            .andExpect(jsonPath("$.onboarding.criteria[0]").value("베스트셀러")).andExpect(jsonPath("$.onboarding.categories[0]").value("소설"))
            .andExpect(jsonPath("$.onboarding.tags[0]").value("SF")).andExpect(jsonPath("$.onboarding.liked_book_ids[1]").value(300))
            .andExpect(jsonPath("$.memories").isEmpty()).andRespond(withSuccess());

        client.createProfile(request);

        server.verify();
    }

    @Test
    void AI가_오류_상태를_응답하면_AI_서비스_호출_실패로_변환한다() {
        server.expect(requestTo(PROFILE_URL)).andRespond(withStatus(HttpStatus.CONFLICT));

        assertThatThrownBy(() -> client.createProfile(profileRequest())).isInstanceOf(CoreException.class)
            .satisfies(exception -> assertThat(((CoreException) exception).errorCode()).isEqualTo(ErrorCode.AI_SERVICE_FAILURE));
    }

    @Test
    void 응답_시간_초과나_연결_실패는_AI_서비스_호출_실패로_변환한다() {
        server.expect(requestTo(PROFILE_URL)).andRespond(withException(new SocketTimeoutException("read timed out")));

        assertThatThrownBy(() -> client.createProfile(profileRequest())).isInstanceOf(CoreException.class)
            .satisfies(exception -> assertThat(((CoreException) exception).errorCode()).isEqualTo(ErrorCode.AI_SERVICE_FAILURE));
    }

    @Test
    void 갱신은_생성과_같은_인증_헤더와_요청_본문으로_PUT_preferences_profile을_요청한다() {
        MDC.put(RequestMdcFilter.REQUEST_ID_MDC_KEY, "request-put-456");
        final AiPersonalizationProfileRequest request = profileRequest();
        server.expect(requestTo(PROFILE_URL)).andExpect(method(HttpMethod.PUT))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + SERVICE_TOKEN)).andExpect(header("X-Request-Id", "request-put-456"))
            .andExpect(jsonPath("$.user_id").value(42))
            .andExpect(jsonPath("$.idempotency_key").value(idempotencyKeyGenerator.generate(request)))
            .andExpect(jsonPath("$.onboarding.reading_times[0]").value("잠들기 전"))
            .andExpect(jsonPath("$.onboarding.liked_book_ids[1]").value(300)).andExpect(jsonPath("$.memories").isEmpty())
            .andRespond(withSuccess());

        client.updateProfile(request);

        server.verify();
    }

    @Test
    void 취향_기억과_이력을_AI_계약의_칸으로_보내고_저장_시각은_마이크로초까지_싣는다() {
        final AiPersonalizationProfileRequest request =
            new AiPersonalizationProfileRequest(42L, List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(new AiPersonalizationProfileMemory("author", "김영하의 문장을 좋아함", 2, List.of(), "김영하",
                    LocalDateTime.of(2026, 9, 4, 0, 12, 30, 123_456_000), List.of(0.02f), 1)),
                new AiPersonalizationProfileHistory(List.of("9788900000011"), List.of("9788900000042"),
                    List.of(new AiPersonalizationProfileHistory.Review("9788900000011", new BigDecimal("4.5"), "울컥했어요"))));
        server.expect(requestTo(PROFILE_URL)).andExpect(method(HttpMethod.PUT)).andExpect(jsonPath("$.memories[0].type").value("author"))
            .andExpect(jsonPath("$.memories[0].strength").value(2)).andExpect(jsonPath("$.memories[0].applies_to").isEmpty())
            .andExpect(jsonPath("$.memories[0].keyword").value("김영하"))
            .andExpect(jsonPath("$.memories[0].created_at").value("2026-09-04T00:12:30.123456"))
            .andExpect(jsonPath("$.memories[0].dim").value(1)).andExpect(jsonPath("$.history.purchased_isbns[0]").value("9788900000011"))
            .andExpect(jsonPath("$.history.library_isbns[0]").value("9788900000042"))
            .andExpect(jsonPath("$.history.reviews[0].rating").value(4.5))
            .andExpect(jsonPath("$.history.reviews[0].content").value("울컥했어요")).andRespond(withSuccess());

        client.updateProfile(request);

        server.verify();
    }

    private static AiPersonalizationProfileRequest profileRequest() {
        return new AiPersonalizationProfileRequest(42L, List.of("잠들기 전"), List.of("베스트셀러"), List.of("소설"), List.of("SF"),
            List.of(100L, 300L));
    }
}
