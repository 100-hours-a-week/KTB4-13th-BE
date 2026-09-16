package com.book.core.domain.sample.model;

import com.book.common.exception.BusinessException;
import com.book.core.domain.sample.exception.SampleErrorCode;

public final class Sample {
    private final Long id;
    private final String name;

    private Sample(Long id, String name) {
        this.id = id;
        this.name = normalizeName(name);
    }

    public static Sample create(String name) {
        return new Sample(null, name);
    }

    public static Sample restore(Long id, String name) {
        if (id == null || id <= 0) {
            throw new BusinessException(SampleErrorCode.INVALID_SAMPLE_ID);
        }
        return new Sample(id, name);
    }

    public static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(SampleErrorCode.INVALID_SAMPLE_NAME);
        }
        String normalized = name.strip();
        if (normalized.length() > 100) {
            throw new BusinessException(SampleErrorCode.INVALID_SAMPLE_NAME);
        }
        return normalized;
    }

    public Long id() { return id; }
    public String name() { return name; }
}
