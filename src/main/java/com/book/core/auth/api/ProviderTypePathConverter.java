package com.book.core.auth.api;

import com.book.core.user.domain.ProviderType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
class ProviderTypePathConverter implements Converter<String, ProviderType> {
    @Override
    public ProviderType convert(final String source) {
        if ("kakao".equals(source)) {
            return ProviderType.KAKAO;
        }
        throw new IllegalArgumentException("지원하지 않는 provider type입니다.");
    }
}
