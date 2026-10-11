package com.book.core.user.application.usecase;

import com.book.core.cart.application.usecase.CreateCartUseCase;
import com.book.core.point.application.usecase.CreatePointBalanceUseCase;
import com.book.core.user.application.command.UserRegistrationCommand;
import com.book.core.user.application.port.UserProviderRepositoryPort;
import com.book.core.user.application.port.UserRepositoryPort;
import com.book.core.user.domain.User;
import com.book.core.user.domain.UserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserRegistrationUseCase {
    private final UserRepositoryPort userRepository;
    private final UserProviderRepositoryPort userProviderRepository;
    private final CreateCartUseCase createCartUseCase;
    private final CreatePointBalanceUseCase createPointBalanceUseCase;

    @Transactional
    public Long execute(final UserRegistrationCommand command) {
        final User savedUser = userRepository.save(User.create(command.nickname()));
        userProviderRepository
            .save(UserProvider.create(savedUser.id(), command.providerType(), command.providerUserId(), command.providerEmail()));
        createCartUseCase.execute(savedUser.id());
        // NOTE: #244 해결 시 회원 가입 초기화의 호출 순서와 트랜잭션 경계를 함께 검토합니다.
        createPointBalanceUseCase.execute(savedUser.id());
        return savedUser.id();
    }
}
