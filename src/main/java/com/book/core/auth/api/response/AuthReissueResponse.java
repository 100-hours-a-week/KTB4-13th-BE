package com.book.core.auth.api.response;

import com.book.core.auth.application.result.AuthReissueResult;

public record AuthReissueResponse(String accessToken) {

    public static AuthReissueResponse from(final AuthReissueResult result) {
        return new AuthReissueResponse(result.accessToken());
    }
}
