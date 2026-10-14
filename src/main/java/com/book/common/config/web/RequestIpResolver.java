package com.book.common.config.web;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;

/** 직접 연결 상대가 신뢰 프록시인 경우에만 X-Forwarded-For를 오른쪽부터 검증합니다. */
@Component
public class RequestIpResolver {
    private final List<IpAddressMatcher> trustedProxies;

    public RequestIpResolver(@Value("${app.request-ip.trusted-proxies:}") final String trustedProxies) {
        this.trustedProxies = Arrays.stream(trustedProxies.split(",")).map(String::strip).filter((final var value) -> !value.isEmpty())
            .map(IpAddressMatcher::new).toList();
    }

    /** 잘못된 전달 헤더는 폐기하며 DNS 조회 없이 IP 리터럴만 처리합니다. */
    public String resolve(final HttpServletRequest request) {
        final String peer = InetAddress.ofLiteral(request.getRemoteAddr()).getHostAddress();
        if (!isTrusted(peer)) {
            return peer;
        }
        final String forwarded = String.join(",", Collections.list(request.getHeaders("X-Forwarded-For")));
        if (forwarded.isBlank() || forwarded.length() > 4096) {
            return peer;
        }
        try {
            final List<String> addresses = Arrays.stream(forwarded.split(",", -1)).map(String::strip)
                .map((final var value) -> InetAddress.ofLiteral(value).getHostAddress()).toList();
            String candidate = peer;
            for (int index = addresses.size() - 1; index >= 0; index--) {
                if (!isTrusted(candidate)) {
                    return candidate;
                }
                candidate = addresses.get(index);
            }
            return candidate;
        } catch (final IllegalArgumentException exception) {
            return peer;
        }
    }

    private boolean isTrusted(final String address) {
        return trustedProxies.stream().anyMatch((final var proxy) -> proxy.matches(address));
    }
}
