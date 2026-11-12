package com.book.core.book.infrastructure.client;

import com.book.core.book.application.port.NewBookImportNotifier;
import com.book.core.book.application.result.NewBookImportReport;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
class DiscordNewBookImportNotifier implements NewBookImportNotifier {
    private final String webhookUrl;
    private final RestClient client;

    DiscordNewBookImportNotifier(@Value("${app.new-book.discord-webhook-url:}") final String webhookUrl) {
        this.webhookUrl = webhookUrl;
        final var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        client = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public void send(final NewBookImportReport report) {
        if (webhookUrl.isBlank()) {
            return;
        }
        final String summary = """
            신간 도서 등록 배치가 %s.
            runId=%s
            소요 시간: %d ms
            후보 ISBN: %d건 | 상세 조회: %d건 | 신규 등록: %d건
            처리 결과: %s
            """.formatted(report.status(), report.runId(), report.elapsedMs(), report.candidateCount(), report.detailCallCount(),
            report.registeredCount(), report.outcomes());
        String content = summary;
        if (report.failed()) {
            content += "실패 사유: " + report.failureReason();
        }
        client.post().uri(URI.create(webhookUrl)).contentType(MediaType.APPLICATION_JSON).body(Map.of("content", content)).retrieve()
            .toBodilessEntity();
    }
}
