package com.book.core.agentchat.api;

import com.book.common.exception.ErrorCode;
import com.book.common.exception.RetryAfterException;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
class ChatSseExecutor {
    private static final int MAX_STREAMS = 8;

    private final ExecutorService workers = boundedPool("chat-sse-work-");
    private final ExecutorService writers = boundedPool("chat-sse-write-");

    void execute(final ChatSseStream stream, final Runnable work) {
        try {
            writers.execute(stream::write);
            workers.execute(work);
        } catch (final RejectedExecutionException failure) {
            stream.close();
            throw new RetryAfterException(ErrorCode.AI_RECOMMENDATION_UNAVAILABLE, 1L);
        }
    }

    private static ExecutorService boundedPool(final String threadPrefix) {
        return new ThreadPoolExecutor(0, MAX_STREAMS, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(),
            Thread.ofPlatform().daemon().name(threadPrefix, 0).factory());
    }

    @PreDestroy
    void shutdown() {
        workers.shutdownNow();
        writers.shutdownNow();
    }
}
