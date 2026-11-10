package com.book.common.config.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.sun.net.httpserver.HttpServer;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.web.client.ResourceAccessException;
import tools.jackson.databind.ObjectMapper;

class AiChatHttpClientTest {
    @Test
    @Timeout(8)
    void 응답이_계속_도착해도_전체_기한에_연결을_닫고_같은_기한으로_재시도하지_않는다() throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        final CountDownLatch disconnected = new CountDownLatch(1);
        server.createContext("/chat", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            try (final var output = exchange.getResponseBody()) {
                for (int count = 0; count < 80; count++) {
                    output.write(": heartbeat\n\n".getBytes(StandardCharsets.UTF_8));
                    output.flush();
                    Thread.sleep(25);
                }
                output.write("event: done\ndata: {}\n\n".getBytes(StandardCharsets.UTF_8));
            } catch (final java.io.IOException failure) {
                disconnected.countDown();
            } catch (final InterruptedException failure) {
                Thread.currentThread().interrupt();
            }
        });
        server.start();
        final AiChatHttpClient http =
            new AiChatHttpClient(new AiServerProperties("http://127.0.0.1:" + server.getAddress().getPort(), "test-token"));
        try {
            final long deadline = System.nanoTime() + Duration.ofMillis(650).toNanos();
            assertThatThrownBy(() -> http.before(deadline).get().uri("/chat").exchange((request, response) -> {
                try (final InputStream body = response.getBody()) {
                    return AiChatEventStream.read(body, new ObjectMapper(), Map.class, text -> {
                    }, ErrorCode.AI_SERVICE_FAILURE);
                }
            })).isInstanceOf(ResourceAccessException.class);
            assertThat(disconnected.await(2, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> http.before(deadline)).isInstanceOfSatisfying(CoreException.class,
                failure -> assertThat(failure.errorCode()).isEqualTo(ErrorCode.AI_RECOMMENDATION_TIMEOUT));
        } finally {
            http.shutdown();
            server.stop(0);
        }
    }
}
