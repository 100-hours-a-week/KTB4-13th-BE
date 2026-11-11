package com.book.core.library.infrastructure.client.s3;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ShelfPhotoProperties.class)
class ShelfPhotoStorageConfig {
    @Bean
    S3Presigner shelfPhotoPresigner(final ShelfPhotoProperties properties) {
        return S3Presigner.builder().region(Region.of(properties.region()))
            .credentialsProvider(DefaultCredentialsProvider.builder().build()).build();
    }
}
