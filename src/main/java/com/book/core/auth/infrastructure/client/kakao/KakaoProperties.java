package com.book.core.auth.infrastructure.client.kakao;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("auth.kakao")
public record KakaoProperties(@NotBlank String clientId) {}
