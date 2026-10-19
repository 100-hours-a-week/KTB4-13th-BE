package com.book.core.auth.application.service;

import com.book.core.cart.application.usecase.CreateCartUseCase;
import com.book.core.point.application.usecase.CreatePointBalanceUseCase;
import com.book.core.user.application.command.UserRegistrationCommand;
import com.book.core.user.application.usecase.UserRegistrationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 회원가입 조율에 한해 Service가 트랜잭션을 소유해 회원·OAuth 연결, 장바구니, 포인트 잔액을 함께 커밋하거나 모두 되돌립니다. */
@Service
@RequiredArgsConstructor
public class SignUpService {
    private final UserRegistrationUseCase userRegistrationUseCase;
    private final CreateCartUseCase createCartUseCase;
    private final CreatePointBalanceUseCase createPointBalanceUseCase;

    @Transactional
    public Long signUp(final UserRegistrationCommand command) {
        final Long userId = userRegistrationUseCase.execute(command);
        createCartUseCase.execute(userId);
        createPointBalanceUseCase.execute(userId);
        return userId;
    }
}
