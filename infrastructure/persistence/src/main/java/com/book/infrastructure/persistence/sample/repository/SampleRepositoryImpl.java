package com.book.infrastructure.persistence.sample.repository;

import com.book.common.exception.BusinessException;
import com.book.common.exception.CommonErrorCode;
import com.book.core.application.sample.port.out.SampleRepository;
import com.book.core.domain.sample.model.Sample;
import com.book.infrastructure.persistence.sample.mapper.SamplePersistenceMapper;
import jakarta.persistence.PersistenceException;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

@Repository
class SampleRepositoryImpl implements SampleRepository {
    private final SampleJpaRepository repository;

    SampleRepositoryImpl(SampleJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Sample save(Sample sample) {
        try {
            return SamplePersistenceMapper.toDomain(
                    repository.saveAndFlush(SamplePersistenceMapper.toEntity(sample)));
        } catch (DataAccessException | PersistenceException exception) {
            throw new BusinessException(CommonErrorCode.STORAGE_FAILURE, exception);
        }
    }

    @Override
    public Optional<Sample> findById(Long id) {
        try {
            return repository.findById(id).map(SamplePersistenceMapper::toDomain);
        } catch (DataAccessException | PersistenceException exception) {
            throw new BusinessException(CommonErrorCode.STORAGE_FAILURE, exception);
        }
    }
}
