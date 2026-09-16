package com.book.infrastructure.persistence.sample.mapper;

import com.book.core.domain.sample.model.Sample;
import com.book.infrastructure.persistence.sample.entity.SampleEntity;

public final class SamplePersistenceMapper {
    private SamplePersistenceMapper() {}

    public static SampleEntity toEntity(Sample sample) {
        return new SampleEntity(sample.id(), sample.name());
    }

    public static Sample toDomain(SampleEntity entity) {
        return Sample.restore(entity.id(), entity.name());
    }
}
