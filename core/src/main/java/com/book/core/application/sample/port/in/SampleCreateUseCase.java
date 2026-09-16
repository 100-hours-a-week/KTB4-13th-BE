package com.book.core.application.sample.port.in;

import com.book.core.application.sample.command.SampleCreateCommand;
import com.book.core.application.sample.port.in.result.SampleCreateResult;

public interface SampleCreateUseCase {
    SampleCreateResult execute(SampleCreateCommand command);
}
