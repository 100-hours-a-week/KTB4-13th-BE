package com.book.core.sample.infrastructure.persistence.repository;

import com.book.core.sample.application.port.SampleRepository;
import com.book.core.sample.domain.Sample;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class SampleRepositoryImpl implements SampleRepository {
    private final SampleJpaRepository repository;

    @Override
    public Sample save(final Sample sample) {
        return repository.saveAndFlush(sample);
    }

    @Override
    public Optional<Sample> findById(final Long id) {
        return repository.findById(id);
    }
}
