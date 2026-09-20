package com.book.core.auth.infrastructure.client.kakao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.book.common.exception.BusinessException;
import com.book.common.exception.ErrorCode;
import com.book.core.auth.domain.exception.AuthErrorCode;
import com.book.core.user.domain.ProviderType;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

class KakaoOAuthTokenClientImplTest {
    private static final String AUTHORIZATION_CODE = "authorization-code";
    private static final String CODE_VERIFIER = "code-verifier";
    private static final String CLIENT_SECRET = "test-kakao-client-secret";

    private MockRestServiceServer server;
    private RestClient.Builder restClientBuilder;
    private KakaoOAuthTokenClientImpl client;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder().baseUrl(KakaoOAuthClientConfiguration.KAKAO_ISSUER);
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        client = createClient(CLIENT_SECRET);
    }

    @AfterEach
    void verifyServer() {
        server.verify();
    }

    @Test
    void 인가_코드와_PKCE_verifier를_교환해_ID_Token만_반환한다() {
        expectTokenRequest(withSuccess(
                "{\"access_token\":\"kakao-access-token\",\"refresh_token\":\"kakao-refresh-token\","
                        + "\"id_token\":\"kakao-id-token\"}",
                MediaType.APPLICATION_JSON));

        final String idToken = client.exchangeForIdToken(ProviderType.KAKAO, AUTHORIZATION_CODE, CODE_VERIFIER);

        assertThat(idToken).isEqualTo("kakao-id-token");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void Client_Secret이_없거나_blank이면_해당_parameter를_제외한다(final String clientSecret) {
        client = createClient(clientSecret);
        expectTokenRequestWithoutClientSecret(withSuccess(
                "{\"access_token\":\"kakao-access-token\",\"refresh_token\":\"kakao-refresh-token\","
                        + "\"id_token\":\"kakao-id-token\"}",
                MediaType.APPLICATION_JSON));

        final String idToken = client.exchangeForIdToken(ProviderType.KAKAO, AUTHORIZATION_CODE, CODE_VERIFIER);

        assertThat(idToken).isEqualTo("kakao-id-token");
    }

    @Test
    void ID_Token이_없는_응답은_INTERNAL_ERROR로_변환한다() {
        expectTokenRequest(withSuccess("{\"access_token\":\"kakao-access-token\"}", MediaType.APPLICATION_JSON));

        assertError(AuthErrorCode.OAUTH_PROVIDER_CONFIGURATION_ERROR, ErrorCode.Category.INTERNAL_ERROR);
    }

    @Test
    void 만료되거나_사용된_인가_코드는_UNAUTHORIZED로_변환한다() {
        expectTokenRequest(withStatus(HttpStatus.BAD_REQUEST)
                .body("{\"error\":\"invalid_grant\",\"error_code\":\"KOE320\"}")
                .contentType(MediaType.APPLICATION_JSON));

        assertError(AuthErrorCode.INVALID_AUTHORIZATION_CODE, ErrorCode.Category.UNAUTHORIZED);
    }

    @Test
    void 잘못된_Client_Secret은_INTERNAL_ERROR로_변환한다() {
        expectTokenRequest(withStatus(HttpStatus.UNAUTHORIZED)
                .body("{\"error\":\"invalid_client\",\"error_code\":\"KOE010\"}")
                .contentType(MediaType.APPLICATION_JSON));

        assertError(AuthErrorCode.OAUTH_PROVIDER_CONFIGURATION_ERROR, ErrorCode.Category.INTERNAL_ERROR);
    }

    @Test
    void 알_수_없는_4xx는_인증_실패로_추측하지_않고_INTERNAL_ERROR로_변환한다() {
        expectTokenRequest(withStatus(HttpStatus.BAD_REQUEST)
                .body("{\"error\":\"invalid_request\",\"error_code\":\"KOE999\"}")
                .contentType(MediaType.APPLICATION_JSON));

        assertError(AuthErrorCode.OAUTH_TOKEN_EXCHANGE_FAILURE, ErrorCode.Category.INTERNAL_ERROR);
    }

    @Test
    void 요청_제한은_EXTERNAL_SERVICE_ERROR로_변환한다() {
        expectTokenRequest(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                .body("{\"error\":\"invalid_request\",\"error_code\":\"KOE237\"}")
                .contentType(MediaType.APPLICATION_JSON));

        assertError(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE, ErrorCode.Category.EXTERNAL_SERVICE_ERROR);
    }

    @Test
    void Kakao_5xx는_EXTERNAL_SERVICE_ERROR로_변환한다() {
        expectTokenRequest(withServerError());

        assertError(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE, ErrorCode.Category.EXTERNAL_SERVICE_ERROR);
    }

    @Test
    void network_실패는_EXTERNAL_SERVICE_ERROR로_변환한다() {
        expectTokenRequest(withException(new IOException("network unavailable")));

        assertError(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE, ErrorCode.Category.EXTERNAL_SERVICE_ERROR);
    }

    private void expectTokenRequest(final ResponseCreator responseCreator) {
        expectTokenRequest(responseCreator, true);
    }

    private void expectTokenRequestWithoutClientSecret(final ResponseCreator responseCreator) {
        expectTokenRequest(responseCreator, false);
    }

    private void expectTokenRequest(final ResponseCreator responseCreator, final boolean includesClientSecret) {
        final MultiValueMap<String, String> expected = new LinkedMultiValueMap<>();
        expected.add("grant_type", "authorization_code");
        expected.add("client_id", "test-kakao-rest-api-key");
        if (includesClientSecret) {
            expected.add("client_secret", CLIENT_SECRET);
        }
        expected.add("redirect_uri", "http://localhost:3000/oauth/kakao/callback");
        expected.add("code", AUTHORIZATION_CODE);
        expected.add("code_verifier", CODE_VERIFIER);
        server.expect(requestTo(KakaoOAuthClientConfiguration.KAKAO_ISSUER + "/oauth/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().formData(expected))
                .andRespond(responseCreator);
    }

    private KakaoOAuthTokenClientImpl createClient(final String clientSecret) {
        return new KakaoOAuthTokenClientImpl(
                restClientBuilder.build(),
                new KakaoProperties(
                        "test-kakao-rest-api-key", clientSecret, "http://localhost:3000/oauth/kakao/callback"));
    }

    private void assertError(final AuthErrorCode errorCode, final ErrorCode.Category category) {
        assertThatThrownBy(() -> client.exchangeForIdToken(ProviderType.KAKAO, AUTHORIZATION_CODE, CODE_VERIFIER))
                .isInstanceOfSatisfying(BusinessException.class, (final var exception) -> {
                    assertThat(exception.errorCode()).isEqualTo(errorCode);
                    assertThat(exception.errorCode().category()).isEqualTo(category);
                });
    }
}
