package com.book.core.auth.application.result;

import java.time.Instant;

public record AuthReissueResult(String accessToken,String refreshToken,Instant refreshExpiresAt){}
