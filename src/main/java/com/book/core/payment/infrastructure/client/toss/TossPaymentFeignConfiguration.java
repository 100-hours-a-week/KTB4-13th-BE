package com.book.core.payment.infrastructure.client.toss;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import feign.Logger;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import java.io.IOException;
import java.util.Set;
import tools.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

class TossPaymentFeignConfiguration {
    @Bean
    RequestInterceptor tossAuthentication(@Value("${payment.toss.secret-key:}") final String secretKey) {
        return template -> {
            if (secretKey.isBlank()) {
                throw new CoreException(ErrorCode.PG_NOT_CONFIGURED);
            }
            final String encoded = Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
            template.header("Authorization", "Basic " + encoded);
        };
    }

    @Bean
    Retryer tossRetryer() {
        return Retryer.NEVER_RETRY;
    }

    @Bean
    Logger.Level tossLoggerLevel() {
        return Logger.Level.NONE;
    }

    @Bean
    ErrorDecoder tossErrorDecoder() {
        return (method, response) -> {
            if (response.status() == 400 && response.body() != null) {
                try (final var input = response.body().asInputStream()) {
                    final byte[] bytes = input.readNBytes(8193);
                    if (bytes.length <= 8192) {
                        final String code = JsonMapper.builder().build().readTree(bytes).path("code").asText();
                        if (Set.of("REJECT_CARD_PAYMENT", "REJECT_CARD_COMPANY", "INVALID_REJECT_CARD").contains(code)) {
                            return new CoreException(ErrorCode.PG_REJECTED);
                        }
                    }
                } catch (final IOException | RuntimeException exception) {
                    return new CoreException(ErrorCode.PG_UNAVAILABLE);
                }
            }
            return new CoreException(ErrorCode.PG_UNAVAILABLE);
        };
    }
}
