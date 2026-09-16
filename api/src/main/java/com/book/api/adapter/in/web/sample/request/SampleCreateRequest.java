package com.book.api.adapter.in.web.sample.request;

import com.book.core.application.sample.command.SampleCreateCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SampleCreateRequest(@NotBlank @Size(max = 100) String name) {
    public SampleCreateRequest {
        if (name != null) {
            name = name.strip();
        }
    }

    public SampleCreateCommand toCommand() {
        return new SampleCreateCommand(name);
    }
}
