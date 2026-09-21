package com.book.core.user.application.usecase;

import com.book.common.exception.BusinessException;
import com.book.core.user.application.command.UserResolveCommand;
import com.book.core.user.application.port.NicknameGenerator;
import com.book.core.user.application.port.UserProviderRepository;
import com.book.core.user.application.port.UserRepository;
import com.book.core.user.application.result.UserResolveResult;
import com.book.core.user.domain.User;
import com.book.core.user.domain.UserProvider;
import com.book.core.user.domain.exception.UserErrorCode;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserResolveUseCase {
    private final UserRepository userRepository;
    private final UserProviderRepository userProviderRepository;
    private final NicknameGenerator nicknameGenerator;
    private final UserRegistrationUseCase userRegistrationUseCase;

    public UserResolveResult execute(final UserResolveCommand command) {
        final Optional<UserProvider> existingProvider =
                userProviderRepository.findActiveByProviderTypeAndProviderUserId(
                        command.providerType(), command.providerUserId());
        if (existingProvider.isPresent()) {
            return resolveExistingUser(existingProvider.orElseThrow());
        }
        return registerNewUser(command);
    }

    private UserResolveResult resolveExistingUser(final UserProvider userProvider) {
        final User user = userRepository
                .findById(userProvider.userId())
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_PROVIDER_USER_NOT_FOUND));
        if (!user.isActive()) {
            throw new BusinessException(UserErrorCode.USER_PROVIDER_USER_INACTIVE);
        }
        return new UserResolveResult(user.id());
    }

    private UserResolveResult registerNewUser(final UserResolveCommand command) {
        while (true) {
            final String nickname = nicknameGenerator.generate();
            try {
                final Long userId = userRegistrationUseCase.execute(command, nickname);
                return new UserResolveResult(userId);
            } catch (final BusinessException exception) {
                if (exception.errorCode() != UserErrorCode.NICKNAME_CONFLICT) {
                    throw exception;
                }
            }
        }
    }
}
