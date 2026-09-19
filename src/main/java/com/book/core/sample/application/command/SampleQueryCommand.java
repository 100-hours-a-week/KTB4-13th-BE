package com.book.core.sample.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;

public record SampleQueryCommand(Long sampleId) {
    public SampleQueryCommand {
        if (sampleId == null || sampleId <= 0) {
            throw new CoreException(ErrorCode.INVALID_SAMPLE_ID);
        }
    }
}
