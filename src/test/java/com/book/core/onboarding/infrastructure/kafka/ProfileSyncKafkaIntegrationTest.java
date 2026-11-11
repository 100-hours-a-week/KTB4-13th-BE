package com.book.core.onboarding.infrastructure.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.book.core.onboarding.application.command.PutOnboardingBooksCommand;
import com.book.core.onboarding.application.command.RecordPersonalizationAgreementCommand;
import com.book.core.onboarding.application.port.AiPersonalizationProfileClient;
import com.book.core.onboarding.application.port.ProfileSyncJobRepositoryPort;
import com.book.core.onboarding.application.usecase.ProcessProfileSyncJobUseCase;
import com.book.core.onboarding.application.usecase.RecordPersonalizationAgreementUseCase;
import com.book.core.onboarding.application.usecase.SaveOnboardingBooksUseCase;
import java.time.Clock;
import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest(properties = {"app.profile-sync.kafka-enabled=true", "spring.kafka.admin.fail-fast=true",
    "spring.kafka.producer.properties.delivery.timeout.ms=2000", "spring.kafka.producer.properties.request.timeout.ms=1000"})
@ExtendWith(OutputCaptureExtension.class)
@ActiveProfiles({"test", "mysql-test"})
@Testcontainers
@Tag("integration")
@Tag("mysql-migration")
class ProfileSyncKafkaIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Container
    static final KafkaContainer kafka = new KafkaContainer("apache/kafka:4.1.2");

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    Clock clock;
    @Autowired
    ConsumerFactory<Object, Object> consumerFactory;
    @Autowired
    TransactionTemplate transaction;
    @Autowired
    RecordPersonalizationAgreementUseCase agree;
    @Autowired
    SaveOnboardingBooksUseCase saveBooks;
    @MockitoSpyBean
    ProfileSyncJobRepositoryPort jobs;
    @Autowired
    ProcessProfileSyncJobUseCase processJob;
    @MockitoBean
    AiPersonalizationProfileClient ai;

    @DynamicPropertySource
    static void broker(final DynamicPropertyRegistry properties) {
        properties.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class KafkaTopics {
        @Bean
        NewTopic profileSyncTopic() {
            return new NewTopic(ProfileSyncKafkaRelay.TOPIC, 1, (short) 1);
        }

        @Bean
        NewTopic profileSyncDeadLetterTopic() {
            return new NewTopic(ProfileSyncKafkaRelay.TOPIC + ".DLT", 1, (short) 1);
        }
    }

    @Test
    void 커밋된_온보딩_작업은_브로커가_복구되면_소비되고_이전_작업의_중복_메시지는_무시한다(final CapturedOutput output) {
        jdbc.update("INSERT INTO users (id, nickname) VALUES (78001, 'Kafka테스트')");
        jdbc.update("INSERT INTO user_onboardings (user_id, onboarding_status) VALUES (78001, 'IN_PROGRESS')");
        final Long userId = 78001L;
        agree.execute(new RecordPersonalizationAgreementCommand(userId));
        final var command = new PutOnboardingBooksCommand(userId, List.of());
        transaction.executeWithoutResult(status -> {
            saveBooks.execute(command);
            assertThat(revision(userId)).isNotBlank();
            status.setRollbackOnly();
        });
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM profile_sync_jobs", Integer.class)).isZero();

        final var docker = DockerClientFactory.instance().client();
        docker.pauseContainerCmd(kafka.getContainerId()).exec();
        try {
            saveBooks.execute(command);
            final String previousRevision = revision(userId);
            await(() -> Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT kafka_publish_at > available_at FROM profile_sync_jobs WHERE user_id = ?
                """, Boolean.class, userId)));
            saveBooks.execute(command);
            assertThat(revision(userId)).isNotEqualTo(previousRevision);
            jobs.markReady(UUID.fromString(previousRevision));
            await(() -> output.getAll().lines().filter(line -> line.contains("Kafka 발행 실패로 전달 기한 뒤 다시 시도합니다.")).count() >= 2);
            assertThat(jobs.findNext(clock.instant())).isEmpty();
            processJob.execute();
            verifyNoInteractions(ai);
        } finally {
            docker.unpauseContainerCmd(kafka.getContainerId()).exec();
        }

        final UUID currentRevision = UUID.fromString(revision(userId));
        doThrow(new DataAccessResourceFailureException("test database failure"),
            new DataAccessResourceFailureException("test database failure"),
            new DataAccessResourceFailureException("test database failure"),
            new DataAccessResourceFailureException("test database failure")).doCallRealMethod().when(jobs).markReady(currentRevision);

        releasePublicationLease(userId);
        final ConsumerRecord<Object, Object> deadLetter = readDeadLetter();
        assertThat(deadLetter.value()).isEqualTo(currentRevision.toString());
        assertThat(deadLetter.headers().lastHeader(KafkaHeaders.DLT_ORIGINAL_TOPIC)).isNotNull();
        assertThat(deadLetter.headers().lastHeader(KafkaHeaders.DLT_EXCEPTION_MESSAGE)).isNull();
        assertThat(deadLetter.headers().lastHeader(KafkaHeaders.DLT_EXCEPTION_STACKTRACE)).isNull();
        assertThat(jobs.findNext(clock.instant())).isEmpty();
        verify(jobs, times(4)).markReady(currentRevision);

        releasePublicationLease(userId);
        await(() -> jobs.findNext(clock.instant()).isPresent());

        final String readyRevision = revision(userId);
        processJob.execute();
        verify(ai).updateProfile(argThat(request -> userId.equals(request.userId())), any());
        jobs.markReady(UUID.fromString(readyRevision));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM profile_sync_jobs", Integer.class)).isZero();
    }

    private ConsumerRecord<Object, Object> readDeadLetter() {
        try (var consumer = consumerFactory.createConsumer("profile-sync-dlt-test")) {
            final var partition = new TopicPartition(ProfileSyncKafkaRelay.TOPIC + ".DLT", 0);
            consumer.assign(List.of(partition));
            consumer.seekToBeginning(List.of(partition));
            final Instant deadline = Instant.now().plusSeconds(15);
            while (Instant.now().isBefore(deadline)) {
                final var records = consumer.poll(Duration.ofMillis(250)).records(partition);
                if (!records.isEmpty()) {
                    return records.getFirst();
                }
            }
        }
        throw new AssertionError("Profile sync dead-letter record was not published");
    }

    private void releasePublicationLease(final Long userId) {
        jdbc.update("UPDATE profile_sync_jobs SET kafka_publish_at = '2000-01-01' WHERE user_id = ? AND kafka_publish_at IS NOT NULL",
            userId);
    }

    private String revision(final Long userId) {
        return jdbc.queryForObject("SELECT revision FROM profile_sync_jobs WHERE user_id = ?", String.class, userId);
    }

    private static void await(final BooleanSupplier condition) {
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            while (!condition.getAsBoolean()) {
                Thread.sleep(Duration.ofMillis(100));
            }
        });
    }
}
