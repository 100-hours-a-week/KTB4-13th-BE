package com.book.core.application.sample.service;

import com.book.common.exception.BusinessException;
import com.book.core.application.sample.command.SampleQueryCommand;
import com.book.core.application.sample.port.in.SampleQueryUseCase;
import com.book.core.application.sample.port.in.result.SampleQueryResult;
import com.book.core.application.sample.port.out.SampleRepository;
import com.book.core.domain.sample.exception.SampleErrorCode;
import com.book.core.domain.sample.model.Sample;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class SampleQueryService implements SampleQueryUseCase {
    private final SampleRepository repository;

    SampleQueryService(SampleRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public SampleQueryResult execute(SampleQueryCommand command) {
        Sample sample = repository.findById(command.sampleId())
                .orElseThrow(() -> new BusinessException(SampleErrorCode.SAMPLE_NOT_FOUND));
        return new SampleQueryResult(sample.id(), sample.name());
    }
}
