package com.book.core.recommendation.infrastructure.client.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class AiServerPropertiesTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withUserConfiguration(TestConfig.class);

    @Test
    void 환경변수에_해당하는_property로부터_설정값을_바인딩한다() {
        contextRunner.withPropertyValues("ai.base-url=http://ai.internal:8000", "ai.service-token=test-service-token").run(context -> {
            final AiServerProperties properties = context.getBean(AiServerProperties.class);
            assertThat(properties.baseUrl()).isEqualTo("http://ai.internal:8000");
            assertThat(properties.serviceToken()).isEqualTo("test-service-token");
        });
    }

    @Test
    void 설정값이_없으면_빈_문자열로_바인딩된다() {
        // ai.yml의 ${AI_BASE_URL:}/${AI_SERVICE_TOKEN:} 기본값이 실제로 만들어내는 빈 문자열을 재현한다.
        // 이 테스트는 ai.yml을 로드하지 않으므로, 그 기본값 해석 결과를 property 값으로 직접 제공한다.
        contextRunner.withPropertyValues("ai.base-url=", "ai.service-token=").run(context -> {
            final AiServerProperties properties = context.getBean(AiServerProperties.class);
            assertThat(properties.baseUrl()).isEqualTo("");
            assertThat(properties.serviceToken()).isEqualTo("");
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AiServerProperties.class)
    static class TestConfig {
    }
}
