package com.book.core.auth.infrastructure.token;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("auth.token")
public record TokenProperties(
        @NotBlank String secret,
        @NotNull Duration accessTokenExpiration,
        @NotNull Duration refreshTokenExpiration) {}
