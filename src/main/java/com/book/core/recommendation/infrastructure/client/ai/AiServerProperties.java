package com.book.core.recommendation.infrastructure.client.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="ai")public record AiServerProperties(String baseUrl,String serviceToken){}
