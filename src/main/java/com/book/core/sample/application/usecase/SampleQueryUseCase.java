package com.book.core.sample.application.usecase;

import com.book.common.exception.BusinessException;
import com.book.core.sample.application.command.SampleQueryCommand;
import com.book.core.sample.application.port.SampleRepository;
import com.book.core.sample.application.result.SampleQueryResult;
import com.book.core.sample.domain.Sample;
import com.book.core.sample.domain.exception.SampleErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SampleQueryUseCase {
    private final SampleRepository repository;

    @Transactional(readOnly = true)
    public SampleQueryResult execute(final SampleQueryCommand command) {
        final Sample sample = repository
                .findById(command.sampleId())
                .orElseThrow(() -> new BusinessException(SampleErrorCode.SAMPLE_NOT_FOUND));
        return new SampleQueryResult(sample.id(), sample.name());
    }
}
