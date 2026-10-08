package com.book.core.user.application.usecase;

import com.book.core.cart.application.usecase.CreateCartUseCase;
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

    @Transactional
    public Long execute(final UserRegistrationCommand command) {
        final User savedUser = userRepository.save(User.create(command.nickname()));
        userProviderRepository
            .save(UserProvider.create(savedUser.id(), command.providerType(), command.providerUserId(), command.providerEmail()));
        createCartUseCase.execute(savedUser.id());
        return savedUser.id();
    }
}
