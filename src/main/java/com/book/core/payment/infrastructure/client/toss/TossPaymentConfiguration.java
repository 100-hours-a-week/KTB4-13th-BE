package com.book.core.payment.infrastructure.client.toss;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration(proxyBeanMethods = false)
class TossPaymentConfiguration {
    private static final int MAX_ERROR_BODY_BYTES = 8192;
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    @Bean
    TossPaymentHttpClient tossPaymentHttpClient(@Value("${payment.toss.base-url}") final String baseUrl,
        @Value("${payment.toss.secret-key:}") final String secretKey,
        @Value("${payment.toss.connect-timeout-ms:3000}") final int connectTimeoutMillis,
        @Value("${payment.toss.read-timeout-ms:10000}") final int readTimeoutMillis) {
        final var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMillis));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMillis));

        final var restClient =
            RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).requestInterceptor((request, body, execution) -> {
                if (secretKey.isBlank()) {
                    throw new CoreException(ErrorCode.PG_NOT_CONFIGURED);
                }
                request.getHeaders().setBasicAuth(secretKey, "", StandardCharsets.UTF_8);
                return execution.execute(request, body);
            }).defaultStatusHandler(status -> status.isError(), TossPaymentConfiguration::handleError).build();

        final var adapter = RestClientAdapter.create(restClient);
        return HttpServiceProxyFactory.builderFor(adapter).build().createClient(TossPaymentHttpClient.class);
    }

    /**
     * Only documented card declines are final; unrecognized PG errors keep the payment outcome
     * unresolved.
     *
     * @see <a href="https://docs.tosspayments.com/reference/error-codes">Toss Payments API error
     *      codes</a>
     */
    private static void handleError(final HttpRequest request, final ClientHttpResponse response) throws IOException {
        if (request.getMethod() == HttpMethod.POST && request.getURI().getPath().endsWith("/v1/payments/confirm")
            && response.getStatusCode().is4xxClientError()) {
            final String code;
            try (final var body = response.getBody()) {
                final byte[] bytes = body.readNBytes(MAX_ERROR_BODY_BYTES + 1);
                if (bytes.length > MAX_ERROR_BODY_BYTES) {
                    throw new IOException("PG error response exceeded the allowed size");
                }
                code = JSON_MAPPER.readTree(bytes).path("code").asText();
            } catch (final IOException | RuntimeException exception) {
                throw new CoreException(ErrorCode.PG_UNAVAILABLE);
            }

            final int status = response.getStatusCode().value();
            if ((status == 400 && "INVALID_REJECT_CARD".equals(code))
                || (status == 403 && ("REJECT_CARD_COMPANY".equals(code) || "REJECT_CARD_PAYMENT".equals(code)))) {
                throw new CoreException(ErrorCode.PG_REJECTED);
            }
        }
        throw new CoreException(ErrorCode.PG_UNAVAILABLE);
    }
}
