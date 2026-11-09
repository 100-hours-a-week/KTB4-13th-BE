package com.book.core.onboarding.infrastructure.scheduler;

import com.book.core.onboarding.application.usecase.ProcessProfileSyncJobUseCase;
import java.sql.Statement;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.profile-sync.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
class ProfileSyncScheduler {
    private final JdbcTemplate jdbc;
    private final ProcessProfileSyncJobUseCase processJob;

    @Bean(defaultCandidate = false, destroyMethod = "shutdown")
    ScheduledExecutorService profileSyncExecutor() {
        return Executors.newSingleThreadScheduledExecutor();
    }

    @Scheduled(fixedDelay = 1000, scheduler = "profileSyncExecutor")
    void run() {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            try (Statement statement = connection.createStatement()) {
                try (var result = statement.executeQuery("SELECT GET_LOCK('book_profile_sync', 0)")) {
                    if (!result.next() || result.getInt(1) != 1) {
                        return null;
                    }
                }
                try {
                    processJob.execute();
                } finally {
                    statement.executeQuery("SELECT RELEASE_LOCK('book_profile_sync')").close();
                }
            }
            return null;
        });
    }
}
