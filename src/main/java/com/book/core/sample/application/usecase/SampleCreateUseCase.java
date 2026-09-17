package com.book.core.sample.application.usecase;

import com.book.core.sample.application.command.SampleCreateCommand;
import com.book.core.sample.application.port.SampleRepository;
import com.book.core.sample.application.result.SampleCreateResult;
import com.book.core.sample.domain.Sample;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SampleCreateUseCase {
    private final SampleRepository repository;

    @Transactional
    public SampleCreateResult execute(final SampleCreateCommand command) {
        final Sample saved = repository.save(Sample.create(command.name()));
        return new SampleCreateResult(saved.id(), saved.name());
    }
}
