package com.book.core.agentchat.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SendChatMessageRequest(@NotBlank @Size(max = 100) String idempotencyKey, @NotBlank @Size(max = 200) String message,
    @NotNull Boolean consented, List<@NotNull @Positive Long> excludeBookIds) {
}
