package com.book.common.config.ai;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class AiChatEventStream {
    private static final int MAX_EVENT_BYTES = 64 * 1024;
    private static final int MAX_RESPONSE_BYTES = 256 * 1024;

    private AiChatEventStream() {}

    /** AI의 최종 envelope를 반환하고, 그 전까지 수신한 말풍선 조각을 전달합니다. */
    public static <T> T read(final InputStream body, final ObjectMapper json, final Class<T> envelopeType, final Consumer<String> onDelta,
        final ErrorCode failure) throws IOException {
        final InputStream input = new BufferedInputStream(body);
        final ByteArrayOutputStream line = new ByteArrayOutputStream();
        final StringBuilder data = new StringBuilder();
        String event = "";
        int responseBytes = 0;
        int eventBytes = 0;
        boolean carriageReturn = false;
        try {
            for (int next; (next = input.read()) != -1;) {
                if (++responseBytes > MAX_RESPONSE_BYTES || ++eventBytes > MAX_EVENT_BYTES) {
                    throw new CoreException(failure);
                }
                if (next == '\n' && carriageReturn) {
                    carriageReturn = false;
                    continue;
                }
                carriageReturn = next == '\r';
                if (!carriageReturn && next != '\n') {
                    line.write(next);
                    continue;
                }
                if (line.size() > 0) {
                    final String field = line.toString(StandardCharsets.UTF_8);
                    if (field.startsWith("event:")) {
                        event = field.substring("event:".length()).stripLeading();
                    } else if (field.startsWith("data:")) {
                        data.append(field.substring("data:".length()).stripLeading()).append('\n');
                    }
                    line.reset();
                    continue;
                }
                if (!data.isEmpty()) {
                    final String payload = data.toString();
                    switch (event) {
                        case "delta" -> onDelta.accept(readText(json, payload, "text", failure));
                        case "done" -> {
                            return requireEnvelope(json.readValue(payload, envelopeType), failure);
                        }
                        case "error" -> throw new CoreException(errorCode(readText(json, payload, "message", failure), failure));
                        default -> {
                        }
                    }
                }
                event = "";
                data.setLength(0);
                eventBytes = 0;
            }
        } catch (final JacksonException exception) {
            throw new CoreException(failure, exception);
        }
        throw new CoreException(failure);
    }

    public static <T> T readJson(final InputStream body, final ObjectMapper json, final Class<T> envelopeType, final ErrorCode failure)
        throws IOException {
        final byte[] bytes = body.readNBytes(MAX_RESPONSE_BYTES + 1);
        if (bytes.length > MAX_RESPONSE_BYTES) {
            throw new CoreException(failure);
        }
        try {
            return requireEnvelope(json.readValue(bytes, envelopeType), failure);
        } catch (final JacksonException exception) {
            throw new CoreException(failure, exception);
        }
    }

    private static <T> T requireEnvelope(final T envelope, final ErrorCode failure) {
        if (envelope == null) {
            throw new CoreException(failure);
        }
        return envelope;
    }

    private static String readText(final ObjectMapper json, final String payload, final String field, final ErrorCode failure) {
        final JsonNode value = json.readTree(payload).path(field);
        if (!value.isTextual()) {
            throw new CoreException(failure);
        }
        return value.asString();
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
