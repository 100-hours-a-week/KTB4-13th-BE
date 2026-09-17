package com.book.core.sample.infrastructure.persistence.mapper;

import com.book.core.sample.domain.Sample;
import com.book.core.sample.infrastructure.persistence.entity.SampleEntity;

public final class SamplePersistenceMapper {
    private SamplePersistenceMapper() {}

    public static SampleEntity toEntity(final Sample sample) {
        return new SampleEntity(sample.id(), sample.name());
    }

    public static Sample toDomain(final SampleEntity entity) {
        return Sample.restore(entity.id(), entity.name());
    }
}
