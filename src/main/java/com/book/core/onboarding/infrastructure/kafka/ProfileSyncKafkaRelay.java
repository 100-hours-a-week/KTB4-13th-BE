package com.book.core.onboarding.infrastructure.kafka;

import com.book.core.onboarding.application.port.ProfileSyncJobRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.profile-sync.kafka-enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
class ProfileSyncKafkaRelay {
    static final String TOPIC = "book.profile-sync.v1";
    private static final long DELIVERY_LEASE_SECONDS = 60;

    private final ProfileSyncJobRepositoryPort jobs;
    private final KafkaTemplate<String, String> kafka;
    private final Clock clock;

    @Bean(defaultCandidate = false, destroyMethod = "shutdown")
    ScheduledExecutorService profileKafkaExecutor() {
        return Executors.newSingleThreadScheduledExecutor();
    }

    // shortcut: 주기마다 한 건씩 발행하고, 적체가 반복되면 여러 건을 한 번에 선점합니다.
    @Scheduled(fixedDelay = 1000, scheduler = "profileKafkaExecutor")
    void publishNext() {
        final Instant now = clock.instant();
        jobs.claimForPublication(now, now.plusSeconds(DELIVERY_LEASE_SECONDS)).ifPresent(this::publish);
    }

    private void publish(final UUID revision) {
        final String message = revision.toString();
        try {
            kafka.send(TOPIC, message, message).whenComplete((result, error) -> {
                if (error != null) {
                    logFailure(error);
                }
            });
        } catch (final RuntimeException exception) {
            logFailure(exception);
        }
    }

    private void logFailure(final Throwable error) {
        log.atWarn().addKeyValue("errorType", error.getClass().getSimpleName()).log("[PROFILE_SYNC] Kafka 발행 실패로 전달 기한 뒤 다시 시도합니다.");
    }

    @KafkaListener(topics = TOPIC, groupId = "book-profile-sync-v1")
    void receive(final String message) {
        final UUID revision;
        try {
            revision = UUID.fromString(message);
            if (!revision.toString().equals(message)) {
                throw new IllegalArgumentException("Invalid profile sync revision");
            }
        } catch (final IllegalArgumentException exception) {
            log.warn("[PROFILE_SYNC] 잘못된 Kafka 작업 ID를 건너뜁니다.");
            return;
        }
        jobs.markReady(revision);
    }
}
