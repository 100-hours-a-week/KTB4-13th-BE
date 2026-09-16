package com.book.infrastructure.persistence.sample.mapper;

import com.book.core.domain.sample.model.Sample;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SamplePersistenceMapperTest {
    @Test
    void 신규_샘플을_ID_없는_Entity로_변환한다() {
        var entity = SamplePersistenceMapper.toEntity(Sample.create("책"));
        assertThat(entity.id()).isNull();
        assertThat(entity.name()).isEqualTo("책");
    }

    @Test
    void 저장된_샘플은_왕복_변환해도_값이_유지된다() {
        var restored = SamplePersistenceMapper.toDomain(
                SamplePersistenceMapper.toEntity(Sample.restore(42L, "책")));
        assertThat(restored.id()).isEqualTo(42L);
        assertThat(restored.name()).isEqualTo("책");
    }
}
