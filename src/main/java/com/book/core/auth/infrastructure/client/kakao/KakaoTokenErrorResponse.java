package com.book.core.auth.infrastructure.client.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
record KakaoTokenErrorResponse(@JsonProperty("error_code") String errorCode) {}
