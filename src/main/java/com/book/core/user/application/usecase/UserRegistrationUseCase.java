package com.book.core.user.application.usecase;

import com.book.core.user.application.command.UserResolveCommand;
import com.book.core.user.application.port.UserProviderRepository;
import com.book.core.user.application.port.UserRepository;
import com.book.core.user.domain.User;
import com.book.core.user.domain.UserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserRegistrationUseCase {
    private final UserRepository userRepository;
    private final UserProviderRepository userProviderRepository;

    @Transactional
    public Long execute(final UserResolveCommand command, final String nickname) {
        final User savedUser = userRepository.save(User.create(nickname));
        userProviderRepository.save(UserProvider.create(
                savedUser.id(), command.providerType(), command.providerUserId(), command.providerEmail()));
        return savedUser.id();
    }
}
