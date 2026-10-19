package com.book.core.auth.infrastructure.security;

import com.book.core.user.application.usecase.CheckUserRestrictionUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

@RequiredArgsConstructor
class RestrictedUserJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final JwtAuthenticationConverter delegate = new JwtAuthenticationConverter();
    private final CheckUserRestrictionUseCase checkUserRestrictionUseCase;

    @Override
    public AbstractAuthenticationToken convert(final Jwt jwt) {
        if (checkUserRestrictionUseCase.execute(Long.valueOf(jwt.getSubject()))) {
            throw new InvalidBearerTokenException("Access Token user is restricted.");
        }
        return delegate.convert(jwt);
    }
}
