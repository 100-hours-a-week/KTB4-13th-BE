package com.book.core.auth.api.request;

import jakarta.validation.constraints.NotBlank;

public record AuthLoginRequest(@NotBlank String authorizationCode,@NotBlank String codeVerifier,@NotBlank String nonce){}
