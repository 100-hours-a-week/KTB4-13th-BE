package com.book.common.notification;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
class DiscordWebhookNotificationSender implements NotificationSender {
    private final String webhookUrl;
    private final RestClient client;

    DiscordWebhookNotificationSender(@Value("${app.notification.discord-webhook-url:}") final String webhookUrl) {
        this.webhookUrl = webhookUrl;
        final var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        client = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public void send(final String message) {
        if (webhookUrl.isBlank()) {
            return;
        }
        try {
            client.post().uri(URI.create(webhookUrl)).contentType(MediaType.APPLICATION_JSON).body(Map.of("content", message)).retrieve()
                .toBodilessEntity();
        } catch (final RestClientException | IllegalArgumentException exception) {
            // 전송 예외에 웹훅 URL이나 외부 응답 원문이 포함될 수 있습니다.
            throw new IllegalStateException("디스코드 웹훅 전송에 실패했습니다.");
        }
    }
}
