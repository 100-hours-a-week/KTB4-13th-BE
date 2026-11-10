package com.book.core.agentchat.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.book.common.exception.ErrorCode;
import com.book.common.exception.RetryAfterException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class ChatSseExecutorTest {
    @Test
    void FE_쓰기가_막혀도_AI_작업은_끝나고_가득_차면_새_요청을_대기시키지_않는다() throws Exception {
        final ChatSseExecutor executor = new ChatSseExecutor();
        final CountDownLatch writersBlocked = new CountDownLatch(8);
        final CountDownLatch releaseWriters = new CountDownLatch(1);
        final CountDownLatch workFinished = new CountDownLatch(8);
        final List<SseEmitter> emitters = new ArrayList<>();
        try {
            for (int index = 0; index < 8; index++) {
                final SseEmitter emitter = mock(SseEmitter.class);
                emitters.add(emitter);
                doAnswer(invocation -> {
                    writersBlocked.countDown();
                    assertThat(releaseWriters.await(5, TimeUnit.SECONDS)).isTrue();
                    return null;
                }).when(emitter).send(any(SseEmitter.SseEventBuilder.class));
                final ChatSseStream stream = new ChatSseStream(emitter);
                executor.execute(stream, () -> {
                    for (int piece = 0; piece < 20; piece++) {
                        stream.delta("먼저 보이는 답변");
                    }
                    stream.finish("done", "저장한 답변");
                    workFinished.countDown();
                });
            }

            assertThat(writersBlocked.await(3, TimeUnit.SECONDS)).isTrue();
            assertThat(workFinished.await(3, TimeUnit.SECONDS)).isTrue();
            final ChatSseStream rejected = new ChatSseStream(new SseEmitter());
            assertThatThrownBy(() -> executor.execute(rejected, () -> {
                throw new AssertionError("거절된 작업이 실행되었습니다");
            })).isInstanceOfSatisfying(RetryAfterException.class,
                failure -> assertThat(failure.errorCode()).isEqualTo(ErrorCode.AI_RECOMMENDATION_UNAVAILABLE));
            assertThat(rejected.isClosed()).isTrue();

            releaseWriters.countDown();
            for (final SseEmitter emitter : emitters) {
                verify(emitter, org.mockito.Mockito.timeout(2000)).complete();
                verify(emitter, atMost(9)).send(any(SseEmitter.SseEventBuilder.class));
            }
        } finally {
            releaseWriters.countDown();
            executor.shutdown();
        }
    }
}
