package com.book.core.sample.application.command;

import com.book.common.exception.BusinessException;
import com.book.core.sample.domain.exception.SampleErrorCode;

public record SampleQueryCommand(Long sampleId) {
    public SampleQueryCommand {
        if (sampleId == null || sampleId <= 0) {
            throw new BusinessException(SampleErrorCode.INVALID_SAMPLE_ID);
        }
    }
}
