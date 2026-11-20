package com.book.core.book.infrastructure.scheduler;

import com.book.core.book.application.service.NewBookImportService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.new-book.enabled", havingValue = "true")
@RequiredArgsConstructor
class NewBookImportScheduler {
    private final NewBookImportService imports;

    @Bean(defaultCandidate = false, destroyMethod = "shutdown")
    static ScheduledExecutorService newBookImportExecutor(
        @Value("${app.new-book.national-library-key}") final String nationalLibraryKey,
        @Value("${app.new-book.data-for-library-key}") final String dataForLibraryKey
    ) {
        if (nationalLibraryKey.isBlank() || dataForLibraryKey.isBlank()) {
            throw new IllegalArgumentException("Both new book catalog API keys are required");
        }
        return Executors.newSingleThreadScheduledExecutor();
    }

    // NOTE: 기본 실행 시각 16:00은 운영 확인용입니다. 확인이 끝나면 API 사용이 적은 새벽 시간으로 바꿔야 합니다.
    @Scheduled(cron = "${app.new-book.collection-cron:0 0 16 * * *}", zone = "Asia/Seoul", scheduler = "newBookImportExecutor")
    void importNewBooks() {
        imports.importNewBooks(LocalDate.now(ZoneId.of("Asia/Seoul")));
    }
}
