package com.book.core.book.infrastructure.client;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BookCatalogProperties.class)
class BookCatalogClientConfiguration {
}
