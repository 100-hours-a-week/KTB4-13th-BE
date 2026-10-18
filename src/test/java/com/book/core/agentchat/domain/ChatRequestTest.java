package com.book.core.agentchat.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ChatRequestTest {
    private static final Instant NOW = Instant.parse("2026-10-18T00:00:00Z");
    private static final Duration LEASE = Duration.ofSeconds(60);

    @Test
    void 종료된_요청은_재개_완료_실패_처리할_수_없다() {
        final ChatRequest request = new ChatRequest(1L, "key", "a".repeat(64), NOW, LEASE);
        request.complete(1, NOW);
        assertThat(request.completed()).isTrue();
        assertThatThrownBy(() -> request.resume(NOW.plusSeconds(60), LEASE)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> request.complete(1, NOW)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> request.fail(1, ErrorCode.AI_RECOMMENDATION_FAILURE)).isInstanceOf(CoreException.class);
    }

    @Test
    void 재시도_메타데이터를_보존하고_만료_경계에서만_인계한다() {
        final ChatRequest request = new ChatRequest(1L, "key", "a".repeat(64), NOW, LEASE);
        assertThat(request.expired(NOW.plusSeconds(59))).isFalse();
        assertThatThrownBy(() -> request.abandon(NOW.plusSeconds(59))).isInstanceOf(CoreException.class);
        assertThat(request.expired(NOW.plusSeconds(60))).isTrue();
        request.resume(NOW.plusSeconds(60), LEASE);
        assertThat(request.generation()).isEqualTo(2);
        request.fail(2, ErrorCode.AI_RECOMMENDATION_RATE_LIMITED, 17L);
        assertThat(request.failureCode()).isEqualTo("AI_RECOMMENDATION_RATE_LIMITED");
        assertThat(request.retryAfterSeconds()).isEqualTo(17);
        assertThat(request.running()).isFalse();
    }

    @Test
    void 원문_대신_유효한_입력_지문과_처리_기한을_요구한다() {
        assertThatThrownBy(() -> new ChatRequest(0L, "key", "a".repeat(64), NOW, LEASE)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new ChatRequest(1L, " ", "a".repeat(64), NOW, LEASE)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new ChatRequest(1L, "key", "text", NOW, LEASE)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new ChatRequest(1L, "key", "a".repeat(64), NOW, Duration.ZERO)).isInstanceOf(CoreException.class);
        final ChatRequest request = new ChatRequest(1L, "key", "a".repeat(64), NOW, LEASE);
        assertThatThrownBy(() -> request.fail(1, null)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> request.fail(1, ErrorCode.AI_RECOMMENDATION_FAILURE, -1L)).isInstanceOf(CoreException.class);
        request.requireSameInput("a".repeat(64));
        assertThatThrownBy(() -> request.requireSameInput("b".repeat(64))).isInstanceOf(CoreException.class);
    }
}
