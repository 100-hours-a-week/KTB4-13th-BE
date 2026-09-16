package com.book.core.application.sample.service;

import com.book.core.application.sample.command.SampleCreateCommand;
import com.book.core.application.sample.port.in.SampleCreateUseCase;
import com.book.core.application.sample.port.in.result.SampleCreateResult;
import com.book.core.application.sample.port.out.SampleRepository;
import com.book.core.domain.sample.model.Sample;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class SampleCreateService implements SampleCreateUseCase {
    private final SampleRepository repository;

    SampleCreateService(SampleRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public SampleCreateResult execute(SampleCreateCommand command) {
        Sample saved = repository.save(Sample.create(command.name()));
        return new SampleCreateResult(saved.id(), saved.name());
    }
}
