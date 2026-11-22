package com.book.core.book.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

class NewBookCollectorPropertiesTest {
    @Test
    void 정보나루_설정은_jeongbo_naru_키에서_읽는다() {
        final var source = new MapConfigurationPropertySource(
            Map.of("app.new-book.jeongbo-naru-key", "jeongbo-key", "app.new-book.jeongbo-naru-base-url", "https://jeongbo.test"));

        final var properties = new Binder(source).bindOrCreate("app.new-book", NewBookCollectorProperties.class);

        assertThat(properties.jeongboNaruKey()).isEqualTo("jeongbo-key");
        assertThat(properties.jeongboNaruBaseUrl()).isEqualTo("https://jeongbo.test");
    }

    @Test
    void 설정이_없으면_기본_주소와_빈_키를_쓴다() {
        final var properties =
            new Binder(new MapConfigurationPropertySource()).bindOrCreate("app.new-book", NewBookCollectorProperties.class);

        assertThat(properties.jeongboNaruBaseUrl()).isEqualTo("https://www.data4library.kr");
        assertThat(properties.jeongboNaruKey()).isEmpty();
        assertThat(properties.nationalLibraryBaseUrl()).isEqualTo("https://www.nl.go.kr");
    }

    @ParameterizedTest
    @CsvSource({
        "JEONGBO_NARU_API_KEY, new-key, app.new-book.jeongbo-naru-key, new-key",
        "DATA_FOR_LIBRARY_API_KEY, old-key, app.new-book.jeongbo-naru-key, old-key",
        "JEONGBO_NARU_DAILY_LIMIT, 700, app.new-book.jeongbo-naru-daily-limit, 700",
        "DATA_FOR_LIBRARY_DAILY_LIMIT, 600, app.new-book.jeongbo-naru-daily-limit, 600"
    })
    void 정보나루_환경_변수는_새_이름을_먼저_읽고_없으면_옛_이름을_읽는다(
        final String variable,
        final String value,
        final String key,
        final String expected
    ) throws IOException {
        assertThat(applicationEnvironment(Map.of(variable, value)).getProperty(key)).isEqualTo(expected);
    }

    @Test
    void 정보나루_환경_변수가_둘_다_있으면_새_이름을_쓴다() throws IOException {
        final var environment = applicationEnvironment(Map.of("JEONGBO_NARU_API_KEY", "new-key", "DATA_FOR_LIBRARY_API_KEY", "old-key"));

        assertThat(environment.getProperty("app.new-book.jeongbo-naru-key")).isEqualTo("new-key");
        assertThat(environment.getProperty("app.new-book.jeongbo-naru-daily-limit")).isEqualTo("500");
    }

    private static StandardEnvironment applicationEnvironment(final Map<String, Object> variables) throws IOException {
        final var environment = new StandardEnvironment();
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().addFirst(new MapPropertySource("variables", variables));
        new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yaml"))
            .forEach(environment.getPropertySources()::addLast);
        return environment;
    }
}
