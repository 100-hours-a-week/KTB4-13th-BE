package com.book.common.config.ai;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import org.springframework.http.client.ClientHttpResponse;

public final class AiChatEventStream {
    private AiChatEventStream() {}

    /** AI의 최종 envelope를 반환하고, 그 전까지 수신한 말풍선 조각을 전달합니다. */
    public static <T> T read(final ClientHttpResponse response, final ObjectMapper json, final Class<T> envelopeType,
        final Consumer<String> onDelta, final ErrorCode failure) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(response.getBody(), StandardCharsets.UTF_8));
        final StringBuilder data = new StringBuilder();
        String event = "";
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("event:")) {
                    event = line.substring("event:".length()).stripLeading();
                } else if (line.startsWith("data:")) {
                    data.append(line.substring("data:".length()).stripLeading()).append('\n');
                } else if (line.isEmpty()) {
                    if (!data.isEmpty()) {
                        final String payload = data.toString();
                        switch (event) {
                            case "delta" -> onDelta.accept(readText(json, payload, "text", failure));
                            case "done" -> {
                                return json.readValue(payload, envelopeType);
                            }
                            case "error" -> throw new CoreException(errorCode(readText(json, payload, "message", failure), failure));
                            default -> {
                            }
                        }
                    }
                    event = "";
                    data.setLength(0);
                }
            }
        } catch (final JsonProcessingException exception) {
            throw new CoreException(failure, exception);
        }
        throw new CoreException(failure);
    }

    private static String readText(final ObjectMapper json, final String payload, final String field, final ErrorCode failure)
        throws JsonProcessingException {
        final JsonNode value = json.readTree(payload).path(field);
        if (!value.isTextual()) {
            throw new CoreException(failure);
        }
        return value.textValue();
    }

    private static ErrorCode errorCode(final String message, final ErrorCode failure) {
        return switch (message) {
            case "unauthorized" -> ErrorCode.AI_SERVICE_UNAUTHORIZED;
            case "rate_limited" -> ErrorCode.AI_RECOMMENDATION_RATE_LIMITED;
            case "upstream_unavailable" -> ErrorCode.AI_RECOMMENDATION_UNAVAILABLE;
            case "generation_timeout" -> ErrorCode.AI_RECOMMENDATION_TIMEOUT;
            case "spec_schema_violation" -> ErrorCode.AI_RECOMMENDATION_SPEC_VIOLATION;
            default -> failure;
        };
    }
}
