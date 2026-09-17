package com.book.core.sample.api.response;

import com.book.core.sample.application.result.SampleQueryResult;

public record SampleQueryResponse(Long id, String name) {
    public static SampleQueryResponse from(final SampleQueryResult result) {
        return new SampleQueryResponse(result.id(), result.name());
    }
}
