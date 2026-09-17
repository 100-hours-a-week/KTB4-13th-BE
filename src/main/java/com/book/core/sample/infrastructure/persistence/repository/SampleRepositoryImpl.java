package com.book.core.sample.infrastructure.persistence.repository;

import com.book.common.exception.BusinessException;
import com.book.common.exception.CommonErrorCode;
import com.book.core.sample.application.port.SampleRepository;
import com.book.core.sample.domain.Sample;
import com.book.core.sample.infrastructure.persistence.mapper.SamplePersistenceMapper;
import jakarta.persistence.PersistenceException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class SampleRepositoryImpl implements SampleRepository {
    private final SampleJpaRepository repository;

    @Override
    public Sample save(final Sample sample) {
        try {
            return SamplePersistenceMapper.toDomain(repository.saveAndFlush(SamplePersistenceMapper.toEntity(sample)));
        } catch (final DataAccessException | PersistenceException exception) {
            throw new BusinessException(CommonErrorCode.STORAGE_FAILURE, exception);
        }
    }

    @Override
    public Optional<Sample> findById(final Long id) {
        try {
            return repository.findById(id).map(SamplePersistenceMapper::toDomain);
        } catch (final DataAccessException | PersistenceException exception) {
            throw new BusinessException(CommonErrorCode.STORAGE_FAILURE, exception);
        }
    }
}
