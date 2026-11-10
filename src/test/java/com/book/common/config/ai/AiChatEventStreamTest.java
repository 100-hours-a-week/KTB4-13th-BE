package com.book.common.config.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class AiChatEventStreamTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void 이벤트와_전체_스트림_크기를_읽는_도중_제한한다() {
        final List<String> deltas = new ArrayList<>();
        final String oversized = "event: delta\ndata: {\"text\":\"" + "x".repeat(64 * 1024) + "\"}\n\n";
        assertThatThrownBy(() -> AiChatEventStream.read(body(oversized), json, Map.class, deltas::add, ErrorCode.AI_SERVICE_FAILURE))
            .isInstanceOfSatisfying(CoreException.class,
                failure -> assertThat(failure.errorCode()).isEqualTo(ErrorCode.AI_SERVICE_FAILURE));
        assertThat(deltas).isEmpty();

        final String heartbeat = ": " + "x".repeat(60 * 1024) + "\n\n";
        final String tooMany = heartbeat.repeat(5) + "event: done\ndata: {}\n\n";
        assertThatThrownBy(() -> AiChatEventStream.read(body(tooMany), json, Map.class, deltas::add, ErrorCode.AI_SERVICE_FAILURE))
            .isInstanceOfSatisfying(CoreException.class,
                failure -> assertThat(failure.errorCode()).isEqualTo(ErrorCode.AI_SERVICE_FAILURE));
    }

    @Test
    void JSON_본문도_크기를_제한한다() {
        final String oversized = "{\"text\":\"" + "x".repeat(256 * 1024) + "\"}";
        assertThatThrownBy(() -> AiChatEventStream.readJson(body(oversized), json, Map.class, ErrorCode.AI_SERVICE_FAILURE))
            .isInstanceOfSatisfying(CoreException.class,
                failure -> assertThat(failure.errorCode()).isEqualTo(ErrorCode.AI_SERVICE_FAILURE));
    }

    private ByteArrayInputStream body(final String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
