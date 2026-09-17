package com.book.core.sample.api.response;

import com.book.core.sample.application.result.SampleCreateResult;

public record SampleCreateResponse(Long id, String name) {
    public static SampleCreateResponse from(final SampleCreateResult result) {
        return new SampleCreateResponse(result.id(), result.name());
    }
}
