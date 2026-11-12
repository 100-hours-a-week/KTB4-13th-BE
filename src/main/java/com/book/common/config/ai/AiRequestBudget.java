package com.book.common.config.ai;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;

public final class AiRequestBudget {
    public static final int CHAT_BODY_BYTES = 64 * 1024;

    private AiRequestBudget() {}

    /** Fits the longest prefix of a newest-first list to the encoded request budget. */
    public static <T, R> R fit(
        final List<T> newestFirst,
        final Function<List<T>, R> encode,
        final ToIntFunction<R> size,
        final int maxBytes,
        final ErrorCode errorCode
    ) {
        final R complete = encode.apply(newestFirst);
        if (size.applyAsInt(complete) <= maxBytes) {
            return complete;
        }
        R fitted = encode.apply(List.of());
        if (size.applyAsInt(fitted) > maxBytes) {
            throw new CoreException(errorCode);
        }
        int low = 1;
        int high = newestFirst.size() - 1;
        while (low <= high) {
            final int count = low + (high - low) / 2;
            final R candidate = encode.apply(newestFirst.subList(0, count));
            if (size.applyAsInt(candidate) <= maxBytes) {
                fitted = candidate;
                low = count + 1;
            } else {
                high = count - 1;
            }
        }
        return fitted;
    }
}
