package com.book.core.payment.infrastructure.client.toss;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import feign.Logger;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.codec.ErrorDecoder;
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
            if (response.status() >= 400 && response.status() < 500 && response.status() != 408 && response.status() != 429) {
                return new CoreException(ErrorCode.PG_REJECTED);
            }
            return new CoreException(ErrorCode.PG_UNAVAILABLE);
        };
    }
}
