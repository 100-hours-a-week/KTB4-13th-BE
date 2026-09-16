package com.book.core.application.sample.port.in;

import com.book.core.application.sample.command.SampleQueryCommand;
import com.book.core.application.sample.port.in.result.SampleQueryResult;

public interface SampleQueryUseCase {
    SampleQueryResult execute(SampleQueryCommand command);
}
