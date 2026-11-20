package com.book.core.book.infrastructure.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.new-book")
record NewBookCollectorProperties(
    @DefaultValue("https://www.nl.go.kr") String nationalLibraryBaseUrl,
    @DefaultValue("") String nationalLibraryKey,
    @DefaultValue("https://www.data4library.kr") String dataForLibraryBaseUrl,
    @DefaultValue("") String dataForLibraryKey) {
}
