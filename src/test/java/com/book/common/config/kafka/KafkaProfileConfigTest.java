package com.book.common.config.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class KafkaProfileConfigTest {
    private final ApplicationContextRunner contextRunner =
        new ApplicationContextRunner().withInitializer(new ConfigDataApplicationContextInitializer());

    @ParameterizedTest
    @CsvSource({"local, book-profile-sync-v1-local", "dev, book-profile-sync-v1-dev", "prod, book-profile-sync-v1-prod"})
    void 프로필별_소비자_그룹이_공통_발행_설정과_함께_적용된다(final String profile, final String group) {
        contextRunner.withPropertyValues("spring.profiles.active=" + profile).run(context -> {
            assertThat(context.getEnvironment().getProperty("app.profile-sync.kafka-group-id")).isEqualTo(group);
            assertThat(context.getEnvironment().getProperty("spring.kafka.producer.acks")).isEqualTo("all");
        });
    }

    @Test
    void 운영_프로필의_소비자_그룹을_환경_설정으로_덮어쓸_수_있다() {
        contextRunner.withPropertyValues("spring.profiles.active=prod", "PROFILE_SYNC_KAFKA_GROUP_ID=book-profile-override")
            .run(context -> assertThat(context.getEnvironment().getProperty("app.profile-sync.kafka-group-id"))
                .isEqualTo("book-profile-override"));
    }
}
