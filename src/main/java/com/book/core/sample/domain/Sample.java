package com.book.core.sample.domain;

import com.book.common.exception.BusinessException;
import com.book.core.sample.domain.exception.SampleErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "samples")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class Sample {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

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
}
