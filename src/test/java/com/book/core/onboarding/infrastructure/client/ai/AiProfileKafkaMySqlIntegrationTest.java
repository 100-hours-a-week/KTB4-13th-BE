package com.book.core.onboarding.infrastructure.client.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

import com.book.common.config.kafka.KafkaTopics;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.onboarding.application.port.AiPersonalizationProfileRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.common.config.ConfigResource;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import com.book.core.onboarding.application.command.PutOnboardingBooksCommand;
import com.book.core.onboarding.application.command.OnboardingPreferenceAnswerCommand;
import com.book.core.onboarding.application.command.PutOnboardingPreferencesCommand;
import com.book.core.onboarding.application.command.RecordPersonalizationAgreementCommand;
import com.book.core.onboarding.application.usecase.ProcessProfileSyncJobUseCase;
import com.book.core.onboarding.application.usecase.RecordPersonalizationAgreementUseCase;
import com.book.core.onboarding.application.usecase.SaveOnboardingBooksUseCase;
import com.book.core.onboarding.application.usecase.UpdateOnboardingPreferencesUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest(properties = {"app.profile-sync.ai-kafka-enabled=true", "app.profile-sync.enabled=false"})
@ActiveProfiles({"test", "mysql-test"})
@Testcontainers
@Tag("integration")
@Tag("mysql-migration")
class AiProfileKafkaMySqlIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");
    private static final String MAX_MESSAGE_BYTES = "max.message.bytes";
    // 운영 토픽에 요구하는 한도입니다. Producer max.request.size와 같습니다.
    private static final String TOPIC_LIMIT = "8454144";

    @Container
    static final KafkaContainer kafka = new KafkaContainer("apache/kafka:4.1.2");

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    RecordPersonalizationAgreementUseCase agree;
    @Autowired
    SaveOnboardingBooksUseCase saveBooks;
    @Autowired
    ProcessProfileSyncJobUseCase processJob;
    @Autowired
    UpdateOnboardingPreferencesUseCase updatePreferences;
    @Autowired
    ConsumerFactory<Object, Object> consumers;
    @Autowired
    ObjectMapper json;
    @Autowired
    KafkaAdmin kafkaAdmin;
    @Autowired
    AiPersonalizationProfileKafkaClient kafkaClient;
    @MockitoSpyBean
    AiPersonalizationProfileClientImpl httpClient;

    @DynamicPropertySource
    static void broker(final DynamicPropertyRegistry properties) {
        properties.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TopicConfig {
        @Bean
        NewTopic aiProfileTopic() {
            return new NewTopic(KafkaTopics.AI_PROFILE, 1, (short) 1).configs(Map.of(MAX_MESSAGE_BYTES, TOPIC_LIMIT));
        }
    }

    @Test
    void 온보딩_작업이_실제_브로커에_전달된_뒤_완료된다() throws Exception {
        final long userId = 78901L;
        jdbc.update("INSERT INTO users (id, nickname) VALUES (?, 'AI Kafka 테스트')", userId);
        jdbc.update("INSERT INTO user_onboardings (user_id, onboarding_status) VALUES (?, 'IN_PROGRESS')", userId);
        agree.execute(new RecordPersonalizationAgreementCommand(userId));
        saveBooks.execute(new PutOnboardingBooksCommand(userId, List.of()));
        final String revision = jdbc.queryForObject("SELECT revision FROM profile_sync_jobs WHERE user_id = ?", String.class, userId);

        processJob.execute();

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM profile_sync_jobs WHERE user_id = ?", Integer.class, userId)).isZero();
        try (var consumer = consumers.createConsumer("ai-profile-test")) {
            final var partition = new TopicPartition(KafkaTopics.AI_PROFILE, 0);
            consumer.assign(List.of(partition));
            consumer.seekToBeginning(List.of(partition));
            final long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
            while (System.nanoTime() < deadline) {
                for (final var record : consumer.poll(Duration.ofMillis(250)).records(partition)) {
                    final var event = json.readTree((String) record.value());
                    if (revision.equals(event.path("job_revision").asText())) {
                        assertThat(record.key()).isEqualTo("78901");
                        assertThat(event.path("profile").path("user_id").asLong()).isEqualTo(userId);
                        return;
                    }
                }
            }
            throw new AssertionError("AI profile record not received");
        }
    }

    @Test
    void 취향을_수정하면_새_작업이_AI_토픽에_발행된다() throws Exception {
        final long userId = 78902L;
        jdbc.update("INSERT INTO users (id, nickname) VALUES (?, 'AI Kafka 취향 테스트')", userId);
        jdbc.update("INSERT INTO user_onboardings (user_id, onboarding_status) VALUES (?, 'IN_PROGRESS')", userId);
        agree.execute(new RecordPersonalizationAgreementCommand(userId));
        saveBooks.execute(new PutOnboardingBooksCommand(userId, List.of()));
        processJob.execute();
        updatePreferences.execute(new PutOnboardingPreferencesCommand(userId,
            List.of(new OnboardingPreferenceAnswerCommand(1L, List.of(2L)), new OnboardingPreferenceAnswerCommand(2L, List.of(6L)),
                new OnboardingPreferenceAnswerCommand(3L, List.of(9L)), new OnboardingPreferenceAnswerCommand(4L, List.of(23L))),
            List.of()));
        final String revision = jdbc.queryForObject("SELECT revision FROM profile_sync_jobs WHERE user_id = ?", String.class, userId);
        processJob.execute();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM profile_sync_jobs WHERE user_id = ?", Integer.class, userId)).isZero();
        try (var consumer = consumers.createConsumer("ai-profile-preference-test")) {
            final var partition = new TopicPartition(KafkaTopics.AI_PROFILE, 0);
            consumer.assign(List.of(partition));
            consumer.seekToBeginning(List.of(partition));
            final long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
            while (System.nanoTime() < deadline) {
                for (final var record : consumer.poll(Duration.ofMillis(250)).records(partition)) {
                    final var event = json.readTree((String) record.value());
                    if (revision.equals(event.path("job_revision").asText())) {
                        assertThat(record.key()).isEqualTo("78902");
                        assertThat(event.at("/profile/onboarding/reading_times/0").asText()).isEqualTo("점심시간이나 짧은 휴식 시간");
                        return;
                    }
                }
            }
            throw new AssertionError("Updated profile record not received");
        }
    }

    @Test
    void 브로커_장애가_나면_작업이_남고_복구_후_다시_발행된다() {
        final long userId = 78903L;
        jdbc.update("INSERT INTO users (id, nickname) VALUES (?, 'AI Kafka 장애 테스트')", userId);
        jdbc.update("INSERT INTO user_onboardings (user_id, onboarding_status) VALUES (?, 'IN_PROGRESS')", userId);
        agree.execute(new RecordPersonalizationAgreementCommand(userId));
        saveBooks.execute(new PutOnboardingBooksCommand(userId, List.of()));
        final var docker = DockerClientFactory.instance().client();
        docker.pauseContainerCmd(kafka.getContainerId()).exec();
        try {
            processJob.execute();
            assertThat(jdbc.queryForObject("SELECT attempts FROM profile_sync_jobs WHERE user_id = ?", Integer.class, userId)).isOne();
        } finally {
            docker.unpauseContainerCmd(kafka.getContainerId()).exec();
        }
        jdbc.update("UPDATE profile_sync_jobs SET available_at = '2000-01-01' WHERE user_id = ?", userId);
        processJob.execute();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM profile_sync_jobs WHERE user_id = ?", Integer.class, userId)).isZero();
    }

    @Test
    void 토픽_한도_안이면_1MiB를_넘는_메시지도_발행하고_토픽_한도가_작으면_크기_초과로_알린다() throws Exception {
        final AiPersonalizationProfileRequest request =
            new AiPersonalizationProfileRequest(78905L, List.of(), List.of(), List.of(), List.of("x".repeat(2 * 1024 * 1024)), List.of());

        kafkaClient.updateProfile(request, UUID.randomUUID());

        assertThat(recordSizes("78905")).singleElement().satisfies(size -> assertThat(size).isGreaterThan(2 * 1024 * 1024));
        try (var admin = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            changeTopicLimit(admin, "1048588");
            try {
                assertThatThrownBy(() -> kafkaClient.updateProfile(request, UUID.randomUUID())).isInstanceOfSatisfying(CoreException.class,
                    exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.AI_PROFILE_MESSAGE_TOO_LARGE));
            } finally {
                changeTopicLimit(admin, TOPIC_LIMIT);
            }
        }
    }

    @Test
    void 최종_탈퇴가_확정된_회원의_작업은_AI_토픽에_발행하지_않고_HTTP로_삭제한다() {
        final long userId = 78904L;
        jdbc.update("INSERT INTO users (id, nickname) VALUES (?, 'AI Kafka 탈퇴 테스트')", userId);
        jdbc.update("INSERT INTO user_onboardings (user_id, onboarding_status) VALUES (?, 'IN_PROGRESS')", userId);
        agree.execute(new RecordPersonalizationAgreementCommand(userId));
        saveBooks.execute(new PutOnboardingBooksCommand(userId, List.of()));
        jdbc.update("UPDATE users SET delete_requested_at = ? WHERE id = ?", LocalDateTime.now().minusDays(8), userId);
        doNothing().when(httpClient).deleteProfile(userId);

        processJob.execute();

        verify(httpClient).deleteProfile(userId);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM profile_sync_jobs WHERE user_id = ?", Integer.class, userId)).isZero();
        assertThat(recordSizes("78904")).isEmpty();
    }

    private List<Integer> recordSizes(final String key) {
        final List<Integer> sizes = new ArrayList<>();
        try (var consumer = consumers.createConsumer("ai-profile-size-" + UUID.randomUUID())) {
            final var partition = new TopicPartition(KafkaTopics.AI_PROFILE, 0);
            consumer.assign(List.of(partition));
            consumer.seekToBeginning(List.of(partition));
            final long end = consumer.endOffsets(List.of(partition)).get(partition);
            final long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
            while (consumer.position(partition) < end && System.nanoTime() < deadline) {
                consumer.poll(Duration.ofMillis(250)).records(partition).stream().filter(record -> key.equals(record.key()))
                    .forEach(record -> sizes.add(record.serializedValueSize()));
            }
            assertThat(consumer.position(partition)).isEqualTo(end);
        }
        return sizes;
    }

    private static void changeTopicLimit(final AdminClient admin, final String limit) throws Exception {
        final var topic = new ConfigResource(ConfigResource.Type.TOPIC, KafkaTopics.AI_PROFILE);
        admin.incrementalAlterConfigs(
            Map.of(topic, List.of(new AlterConfigOp(new ConfigEntry(MAX_MESSAGE_BYTES, limit), AlterConfigOp.OpType.SET)))).all().get();
    }
}
