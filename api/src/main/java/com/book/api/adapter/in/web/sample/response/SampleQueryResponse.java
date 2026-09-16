package com.book.api.adapter.in.web.sample.response;

import com.book.core.application.sample.port.in.result.SampleQueryResult;

public record SampleQueryResponse(Long id, String name) {
    public static SampleQueryResponse from(SampleQueryResult result) {
        return new SampleQueryResponse(result.id(), result.name());
    }
}
