package com.book.core.application.sample.port.out;

import com.book.core.domain.sample.model.Sample;
import java.util.Optional;

public interface SampleRepository {
    Sample save(Sample sample);
    Optional<Sample> findById(Long id);
}
