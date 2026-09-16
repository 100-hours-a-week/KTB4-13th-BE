package com.book.core.application.sample.command;

import com.book.core.domain.sample.model.Sample;

public record SampleCreateCommand(String name) {
    public SampleCreateCommand {
        name = Sample.normalizeName(name);
    }
}
