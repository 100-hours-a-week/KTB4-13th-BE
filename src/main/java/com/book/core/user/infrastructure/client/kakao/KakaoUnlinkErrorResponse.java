package com.book.core.user.infrastructure.client.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record KakaoUnlinkErrorResponse(Integer code) {
}
