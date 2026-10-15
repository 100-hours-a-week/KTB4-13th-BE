package com.book.core.user.api.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateMyInfoRequest(@NotBlank String nickname) {
}
