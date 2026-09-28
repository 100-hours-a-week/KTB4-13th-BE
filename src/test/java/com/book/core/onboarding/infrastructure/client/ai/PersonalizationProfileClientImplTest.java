package com.book.core.onboarding.infrastructure.client.ai;

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
import com.book.core.onboarding.application.port.PersonalizationProfileRequest;
import com.book.core.onboarding.application.port.PersonalizationProfileResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class PersonalizationProfileClientImplTest {
    private static final String SERVICE_TOKEN = "super-secret-profile-token";
    private static final String BASE_URL = "http://ai.internal:8000";

    @Test
    void Authorization_Bearer_header를_포함한다() {
        final RestClient.Builder builder = restClientBuilder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final PersonalizationProfileClientImpl client = new PersonalizationProfileClientImpl(builder.build());

        server.expect(requestTo(BASE_URL + "/preferences/profile")).andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + SERVICE_TOKEN))
            .andRespond(withSuccess(successBody(), MediaType.APPLICATION_JSON));

        client.createProfile(request());

        server.verify();
    }

    @Test
    void 요청_필드를_snake_case로_매핑하고_memories는_항상_빈_배열이다() {
        final RestClient.Builder builder = restClientBuilder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final PersonalizationProfileClientImpl client = new PersonalizationProfileClientImpl(builder.build());

        server.expect(requestTo(BASE_URL + "/preferences/profile")).andExpect(jsonPath("$.user_id").value(42))
            .andExpect(jsonPath("$.idempotency_key").value("profile:42:abc"))
            .andExpect(jsonPath("$.onboarding.reading_times[0]").value("아침")).andExpect(jsonPath("$.onboarding.criteria[0]").value("평점"))
            .andExpect(jsonPath("$.onboarding.categories[0]").value("소설")).andExpect(jsonPath("$.onboarding.tags[0]").value("SF"))
            .andExpect(jsonPath("$.onboarding.liked_book_ids[0]").value(10)).andExpect(jsonPath("$.memories").isEmpty())
            .andRespond(withSuccess(successBody(), MediaType.APPLICATION_JSON));

        client.createProfile(request());

        server.verify();
    }

    @Test
    void 정상_응답을_결과로_매핑한다() {
        final RestClient.Builder builder = restClientBuilder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final PersonalizationProfileClientImpl client = new PersonalizationProfileClientImpl(builder.build());

        server.expect(requestTo(BASE_URL + "/preferences/profile")).andRespond(withSuccess(successBody(), MediaType.APPLICATION_JSON));

        final PersonalizationProfileResult result = client.createProfile(request());

        assertThat(result.coldStart()).isFalse();
        assertThat(result.profileVersion()).isEqualTo(3);
    }

    @Test
    void 상태코드_400은_AI_PROFILE_INVALID_REQUEST로_변환한다() {
        assertErrorMapping(HttpStatus.BAD_REQUEST, ErrorCode.AI_PROFILE_INVALID_REQUEST);
    }

    @Test
    void 상태코드_401은_AI_SERVICE_UNAUTHORIZED로_변환한다() {
        assertErrorMapping(HttpStatus.UNAUTHORIZED, ErrorCode.AI_SERVICE_UNAUTHORIZED);
    }

    @Test
    void 상태코드_409는_AI_PROFILE_IDEMPOTENCY_CONFLICT로_변환한다() {
        assertErrorMapping(HttpStatus.CONFLICT, ErrorCode.AI_PROFILE_IDEMPOTENCY_CONFLICT);
    }

    @Test
    void 상태코드_413은_AI_PROFILE_PAYLOAD_TOO_LARGE로_변환한다() {
        assertErrorMapping(HttpStatus.PAYLOAD_TOO_LARGE, ErrorCode.AI_PROFILE_PAYLOAD_TOO_LARGE);
    }

    @Test
    void 상태코드_500은_AI_PROFILE_FAILURE로_변환한다() {
        assertErrorMapping(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.AI_PROFILE_FAILURE);
    }

    @Test
    void 연결_타임아웃은_AI_PROFILE_TIMEOUT으로_변환한다() {
        final RestClient.Builder builder =
            RestClient.builder().baseUrl("http://127.0.0.1:1").defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + SERVICE_TOKEN);
        final PersonalizationProfileClientImpl client = new PersonalizationProfileClientImpl(builder.build());

        assertThatThrownBy(() -> client.createProfile(request())).isInstanceOf(CoreException.class)
            .satisfies(exception -> assertThat(((CoreException) exception).errorCode()).isEqualTo(ErrorCode.AI_PROFILE_TIMEOUT));
    }

    @Test
    void 예외_메시지에_service_token_원문을_노출하지_않는다() {
        final RestClient.Builder builder = restClientBuilder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final PersonalizationProfileClientImpl client = new PersonalizationProfileClientImpl(builder.build());

        server.expect(requestTo(BASE_URL + "/preferences/profile")).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.createProfile(request())).hasMessageNotContaining(SERVICE_TOKEN);
    }

    private void assertErrorMapping(final HttpStatus status, final ErrorCode expected) {
        final RestClient.Builder builder = restClientBuilder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final PersonalizationProfileClientImpl client = new PersonalizationProfileClientImpl(builder.build());

        server.expect(requestTo(BASE_URL + "/preferences/profile")).andRespond(withStatus(status));

        assertThatThrownBy(() -> client.createProfile(request())).isInstanceOf(CoreException.class)
            .satisfies(exception -> assertThat(((CoreException) exception).errorCode()).isEqualTo(expected));

        server.verify();
    }

    private RestClient.Builder restClientBuilder() {
        return RestClient.builder().baseUrl(BASE_URL).defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + SERVICE_TOKEN);
    }

    private PersonalizationProfileRequest request() {
        return new PersonalizationProfileRequest(42L, "profile:42:abc", List.of("아침"), List.of("평점"), List.of("소설"), List.of("SF"),
            List.of(10L));
    }

    private String successBody() {
        return """
            {
              "message": "profile_success",
              "data": {"cold_start": false, "profile_version": 3}
            }
            """;
    }
}
