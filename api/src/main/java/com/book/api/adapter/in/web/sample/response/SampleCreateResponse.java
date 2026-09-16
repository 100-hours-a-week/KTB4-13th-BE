package com.book.api.adapter.in.web.sample.response;

import com.book.core.application.sample.port.in.result.SampleCreateResult;

public record SampleCreateResponse(Long id, String name) {
    public static SampleCreateResponse from(SampleCreateResult result) {
        return new SampleCreateResponse(result.id(), result.name());
    }
}
