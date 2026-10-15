package com.book.core.user.infrastructure.client.kakao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.book.common.exception.BusinessException;
import com.book.common.exception.ErrorCode;
import com.book.core.user.domain.ProviderType;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

class KakaoUnlinkClientImplTest {
    private MockRestServiceServer server;
    private KakaoUnlinkClientImpl client;

    @BeforeEach
    void setUp() {
        final RestClient.Builder builder = RestClient.builder().baseUrl(KakaoUnlinkClientConfiguration.KAKAO_API_BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new KakaoUnlinkClientImpl(builder.build(), new KakaoUnlinkProperties("admin-key"));
    }

    @AfterEach
    void verifyServer() {
        server.verify();
    }

    @Test
    void 어드민_키와_회원번호로_연결을_해제한다() {
        expectUnlinkRequest(withSuccess("{\"id\":123456789}", MediaType.APPLICATION_JSON));

        assertThatCode(() -> client.unlink(ProviderType.KAKAO, "123456789")).doesNotThrowAnyException();
    }

    @Test
    void 이미_연결이_해제된_사용자는_성공으로_처리한다() {
        expectUnlinkRequest(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
            .body("{\"msg\":\"NotRegisteredUser\",\"code\":-101}"));

        assertThatCode(() -> client.unlink(ProviderType.KAKAO, "123456789")).doesNotThrowAnyException();
    }

    @Test
    void 서버_오류나_타임아웃이면_성공으로_처리하지_않고_예외를_전파한다() {
        expectUnlinkRequest(withServerError());
        expectUnlinkRequest(withException(new SocketTimeoutException("read timed out")));

        assertThatThrownBy(() -> client.unlink(ProviderType.KAKAO, "123456789")).isInstanceOfSatisfying(BusinessException.class,
            (final var exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.OAUTH_PROVIDER_UNAVAILABLE));
        assertThatThrownBy(() -> client.unlink(ProviderType.KAKAO, "123456789")).isInstanceOfSatisfying(BusinessException.class,
            (final var exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.OAUTH_PROVIDER_UNAVAILABLE));
    }

    @Test
    void 성공_응답의_회원번호가_요청과_다르면_성공으로_처리하지_않는다() {
        expectUnlinkRequest(withSuccess("{\"id\":987654321}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.unlink(ProviderType.KAKAO, "123456789")).isInstanceOf(BusinessException.class);
    }

    private void expectUnlinkRequest(final ResponseCreator response) {
        final MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("target_id_type", "user_id");
        form.add("target_id", "123456789");
        server.expect(requestTo(KakaoUnlinkClientConfiguration.KAKAO_API_BASE_URL + "/v1/user/unlink")).andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "KakaoAK admin-key")).andExpect(content().formData(form)).andRespond(response);
    }
}
