package com.book.common.config.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="ai")public record AiServerProperties(String baseUrl,String serviceToken){}
