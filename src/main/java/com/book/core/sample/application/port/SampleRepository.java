package com.book.core.sample.application.port;

import com.book.core.sample.domain.Sample;
import java.util.Optional;

public interface SampleRepository {
    Sample save(final Sample sample);

    Optional<Sample> findById(final Long id);
}
