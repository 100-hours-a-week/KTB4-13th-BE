package com.book.core.sample.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.sample.domain.Sample;
import org.junit.jupiter.api.Test;

class SamplePersistenceMapperTest {
    @Test
    void 신규_샘플을_ID_없는_Entity로_변환한다() {
        final var entity = SamplePersistenceMapper.toEntity(Sample.create("책"));
        assertThat(entity.id()).isNull();
        assertThat(entity.name()).isEqualTo("책");
    }

    @Test
    void 저장된_샘플은_왕복_변환해도_값이_유지된다() {
        final var restored =
                SamplePersistenceMapper.toDomain(SamplePersistenceMapper.toEntity(Sample.restore(42L, "책")));
        assertThat(restored.id()).isEqualTo(42L);
        assertThat(restored.name()).isEqualTo("책");
    }
}
