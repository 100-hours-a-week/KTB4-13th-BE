package com.book.core.sample.domain;

import com.book.common.exception.BusinessException;
import com.book.core.sample.domain.exception.SampleErrorCode;

public final class Sample {
    private final Long id;
    private final String name;

    private Sample(final Long id, final String name) {
        this.id = id;
        this.name = normalizeName(name);
    }

    public static Sample create(final String name) {
        return new Sample(null, name);
    }

    public static Sample restore(final Long id, final String name) {
        if (id == null || id <= 0) {
            throw new BusinessException(SampleErrorCode.INVALID_SAMPLE_ID);
        }
        return new Sample(id, name);
    }

    public static String normalizeName(final String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(SampleErrorCode.INVALID_SAMPLE_NAME);
        }
        final String normalized = name.strip();
        if (normalized.length() > 100) {
            throw new BusinessException(SampleErrorCode.INVALID_SAMPLE_NAME);
        }
        return normalized;
    }

    public Long id() {
        return id;
    }

    public String name() {
        return name;
    }
}
