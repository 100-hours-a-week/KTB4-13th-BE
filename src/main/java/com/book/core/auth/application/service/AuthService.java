package com.book.core.auth.application.service;

import com.book.core.auth.application.command.AuthLoginCommand;
import com.book.core.auth.application.result.AuthLoginResult;
import com.book.core.auth.application.usecase.AuthLoginUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthLoginUseCase authLoginUseCase;

    public AuthLoginResult login(final AuthLoginCommand command) {
        return authLoginUseCase.execute(command);
    }
}
