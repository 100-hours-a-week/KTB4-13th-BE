package com.book.core.library.infrastructure.client.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.common.config.ai.AiRestClientConfig;
import com.book.common.config.ai.AiServerProperties;
import com.book.core.library.application.result.ShelfScanResult;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** 실제 소켓으로 31초 늦은 응답을 받아 사진 전용 읽기 제한이 기존 30초 제한보다 길게 적용되는지 확인합니다. */
@Tag("integration")
class ShelfScanSlowResponseIntegrationTest {
    private static final Duration AI_DELAY = Duration.ofSeconds(31);

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void AI가_30초를_넘겨_응답해도_사진_전용_읽기_제한_안이면_결과를_받는다() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/shelf/scans", exchange -> {
            try {
                Thread.sleep(AI_DELAY);
            } catch (final InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            final byte[] body = """
                {"message":"shelf_scan_success","data":{"photo_type":"cover","items":[]}}
                """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream response = exchange.getResponseBody()) {
                response.write(body);
            }
        });
        server.start();
        final ShelfScanClientImpl client = new ShelfScanClientImpl(new AiRestClientConfig()
            .shelfScanAiRestClient(new AiServerProperties("http://127.0.0.1:" + server.getAddress().getPort(), "service-token")));
        final long startedAt = System.nanoTime();

        final ShelfScanResult scan = client.scan(42L, "https://bucket.s3/shelf-photos/42/a.jpg");

        assertThat(scan).isEqualTo(new ShelfScanResult("cover", List.of()));
        assertThat(Duration.ofNanos(System.nanoTime() - startedAt)).isGreaterThanOrEqualTo(AI_DELAY);
    }
}
