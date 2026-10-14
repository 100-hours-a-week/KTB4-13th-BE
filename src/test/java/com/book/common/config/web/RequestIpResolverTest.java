package com.book.common.config.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class RequestIpResolverTest {
    @Test
    void 기본값은_전달_헤더를_신뢰하지_않는다() {
        final var request = request("192.0.2.10", "198.51.100.1");
        assertThat(new RequestIpResolver("").resolve(request)).isEqualTo("192.0.2.10");
    }

    @Test
    void 신뢰하지_않는_직접_연결의_헤더를_무시한다() {
        assertThat(new RequestIpResolver("10.0.0.0/8").resolve(request("192.0.2.10", "198.51.100.1"))).isEqualTo("192.0.2.10");
    }

    @Test
    void 오른쪽부터_신뢰_프록시를_건너뛰고_위조된_왼쪽_주소를_무시한다() {
        final var request = request("10.0.0.1", "203.0.113.99, 198.51.100.1, 10.0.0.2");
        assertThat(new RequestIpResolver("10.0.0.0/8").resolve(request)).isEqualTo("198.51.100.1");
    }

    @Test
    void 여러_헤더와_IPv6_프록시를_처리한다() {
        final var request = request("2001:db8::1", "198.51.100.1");
        request.addHeader("X-Forwarded-For", "2001:db8::2");
        assertThat(new RequestIpResolver("2001:db8::/32").resolve(request)).isEqualTo("198.51.100.1");
    }

    @Test
    void 누락되거나_잘못된_전달_헤더는_직접_연결_IP로_대체한다() {
        final var resolver = new RequestIpResolver("10.0.0.0/8");
        assertThat(resolver.resolve(request("10.0.0.1", ""))).isEqualTo("10.0.0.1");
        assertThat(resolver.resolve(request("10.0.0.1", "198.51.100.1, invalid"))).isEqualTo("10.0.0.1");
        assertThat(resolver.resolve(request("10.0.0.1", "198.51.100.1,"))).isEqualTo("10.0.0.1");
        assertThat(resolver.resolve(request("10.0.0.1", "x".repeat(4097)))).isEqualTo("10.0.0.1");
    }

    private MockHttpServletRequest request(final String remote, final String forwarded) {
        final var request = new MockHttpServletRequest();
        request.setRemoteAddr(remote);
        request.addHeader("X-Forwarded-For", forwarded);
        return request;
    }
}
