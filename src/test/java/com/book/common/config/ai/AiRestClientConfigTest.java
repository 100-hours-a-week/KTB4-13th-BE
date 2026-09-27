package com.book.common.config.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AiRestClientConfigTest {
    private static final String SECRET_TOKEN = "super-secret-ai-token";
    private static final AiServerProperties PROPERTIES = new AiServerProperties("http://ai.internal:8000", SECRET_TOKEN);

    @Test
    void 모든_요청에_Authorization_Bearer_service_token을_자동으로_붙인다() {
        final RestClient.Builder builder = AiRestClientConfig.restClientBuilder(PROPERTIES);
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final RestClient client = builder.build();

        server.expect(requestTo("http://ai.internal:8000/health")).andExpect(header("Authorization", "Bearer " + SECRET_TOKEN))
            .andRespond(withSuccess());

        client.get().uri("/health").retrieve().toBodilessEntity();

        server.verify();
    }

    @Test
    void AI_401은_사용자_인증_401이_아닌_AI_SERVICE_UNAUTHORIZED로_변환한다() {
        final RestClient.Builder builder = AiRestClientConfig.restClientBuilder(PROPERTIES);
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final RestClient client = builder.build();

        server.expect(requestTo("http://ai.internal:8000/health")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.get().uri("/health").retrieve().toBodilessEntity()).isInstanceOf(CoreException.class)
            .satisfies(exception -> assertThat(((CoreException) exception).errorCode()).isEqualTo(ErrorCode.AI_SERVICE_UNAUTHORIZED));
    }

    @Test
    void AI_401_변환_예외는_사용자_UNAUTHORIZED_코드와_다르다() {
        assertThat(ErrorCode.AI_SERVICE_UNAUTHORIZED).isNotEqualTo(ErrorCode.UNAUTHORIZED);
        assertThat(ErrorCode.AI_SERVICE_UNAUTHORIZED.status()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void 예외_메시지에_service_token_원문을_노출하지_않는다() {
        final RestClient.Builder builder = AiRestClientConfig.restClientBuilder(PROPERTIES);
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final RestClient client = builder.build();

        server.expect(requestTo("http://ai.internal:8000/health")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.get().uri("/health").retrieve().toBodilessEntity()).hasMessageNotContaining(SECRET_TOKEN);
    }
}
