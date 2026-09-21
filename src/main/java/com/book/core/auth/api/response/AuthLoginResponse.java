package com.book.core.auth.api.response;

import com.book.core.auth.application.result.AuthLoginResult;

public record AuthLoginResponse(String accessToken) {
    public static AuthLoginResponse from(final AuthLoginResult result) {
        return new AuthLoginResponse(result.accessToken());
    }
}
