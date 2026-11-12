package com.book.common.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class DiscordWebhookNotificationSenderTest {
    @Test
    void 전달받은_메시지를_웹훅_content에_담아_전송한다() throws Exception {
        final var request = new AtomicReference<String>();
        final var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/webhook", (final var exchange) -> {
            request.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();
        try {
            final var sender = new DiscordWebhookNotificationSender("http://127.0.0.1:" + server.getAddress().getPort() + "/webhook");
            final String message = "완성된 알림 메시지\n둘째 줄";
            sender.send(message);

            final var payload = JsonMapper.builder().build().readTree(request.get());
            assertThat(payload.path("content").asString()).isEqualTo(message);
        } finally {
            server.stop(0);
        }
    }
}
