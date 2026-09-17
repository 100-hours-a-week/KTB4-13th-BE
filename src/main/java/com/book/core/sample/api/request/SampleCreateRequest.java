package com.book.core.sample.api.request;

import com.book.core.sample.application.command.SampleCreateCommand;
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
