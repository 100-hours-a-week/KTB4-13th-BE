package com.book.core.sample.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorType;

public record SampleQueryCommand(Long sampleId) {
    public SampleQueryCommand {
        if (sampleId == null || sampleId <= 0) {
            throw new CoreException(ErrorType.INVALID_SAMPLE_ID);
        }
    }
}
