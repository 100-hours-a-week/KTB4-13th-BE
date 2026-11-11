package com.book.common.config.ai;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import jakarta.annotation.PreDestroy;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AiChatHttpClient {
    private static final Duration TIME_BUDGET = Duration.ofSeconds(50);

    private final HttpClient transport =
        HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(3)).build();
    private final RestClient baseClient;

    public AiChatHttpClient(final AiServerProperties properties) {
        baseClient = RestClient.builder().baseUrl(properties.baseUrl())
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.serviceToken()).build();
    }

    public static long deadline() {
        return System.nanoTime() + TIME_BUDGET.toNanos();
    }

    public RestClient before(final long deadline) {
        final long remaining = deadline - System.nanoTime();
        if (remaining <= 0) {
            throw new CoreException(ErrorCode.AI_RECOMMENDATION_TIMEOUT);
        }
        final JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(transport);
        factory.setReadTimeout(Duration.ofMillis(Math.max(1, Duration.ofNanos(remaining).toMillis())));
        return baseClient.mutate().requestFactory(factory).build();
    }

    @PreDestroy
    void shutdown() {
        transport.shutdownNow();
    }
}
