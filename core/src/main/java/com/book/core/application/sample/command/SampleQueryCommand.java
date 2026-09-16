package com.book.core.application.sample.command;

import com.book.common.exception.BusinessException;
import com.book.core.domain.sample.exception.SampleErrorCode;

public record SampleQueryCommand(Long sampleId) {
    public SampleQueryCommand {
        if (sampleId == null || sampleId <= 0) {
            throw new BusinessException(SampleErrorCode.INVALID_SAMPLE_ID);
        }
    }
}
