package com.book.core.sample.application.command;

import com.book.core.sample.domain.Sample;

public record SampleCreateCommand(String name) {
    public SampleCreateCommand {
        name = Sample.normalizeName(name);
    }
}
