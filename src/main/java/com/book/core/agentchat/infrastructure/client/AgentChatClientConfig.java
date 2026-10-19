package com.book.core.agentchat.infrastructure.client;

import com.book.common.config.ai.AiServerProperties;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AgentChatProperties.class)
class AgentChatClientConfig {
    @Bean("agentShoppingRestClient")
    RestClient agentShoppingRestClient(final AiServerProperties properties) {
        final SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(35));
        return RestClient.builder().baseUrl(properties.baseUrl()).requestFactory(factory)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.serviceToken()).build();
    }
}
