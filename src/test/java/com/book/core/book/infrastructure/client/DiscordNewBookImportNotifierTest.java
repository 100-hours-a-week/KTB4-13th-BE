package com.book.core.book.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.book.application.result.NewBookImportOutcome;
import com.book.core.book.application.result.NewBookImportReport;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class DiscordNewBookImportNotifierTest {
    @Test
    void 웹훅에_실행_시간과_실패_시점까지의_집계를_전송한다() throws Exception {
        final var request = new AtomicReference<String>();
        final var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/webhook", (final var exchange) -> {
            request.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();
        try {
            final var notifier = new DiscordNewBookImportNotifier("http://127.0.0.1:" + server.getAddress().getPort() + "/webhook");
            final var runId = UUID.randomUUID();
            notifier.send(new NewBookImportReport(runId, 1234L, 5, 3, Map.of(NewBookImportOutcome.REGISTERED, 2), "저장 오류"));

            final var payload = JsonMapper.builder().build().readTree(request.get());
            assertThat(payload.path("content").asString()).contains("실패했습니다", "runId=" + runId, "1234 ms", "후보 ISBN: 5건", "상세 조회: 3건",
                "신규 등록: 2건", "실패 사유: 저장 오류");
        } finally {
            server.stop(0);
        }
    }
}
