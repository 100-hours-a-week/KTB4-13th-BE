package com.book.core.auth.infrastructure.client.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record KakaoUnlinkErrorResponse(Integer code) {
}
