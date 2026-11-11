package com.book.core.library.infrastructure.client.s3;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.shelf-photo")
record ShelfPhotoProperties(String bucket, String region) {
}
