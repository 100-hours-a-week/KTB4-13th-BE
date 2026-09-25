package com.book.core.user.application.usecase;

import com.book.common.exception.BusinessException;
import com.book.common.exception.ErrorCode;
import com.book.core.user.application.command.IdentifyUserCommand;
import com.book.core.user.application.command.UserRegistrationCommand;
import com.book.core.user.application.port.NicknameGenerator;
import com.book.core.user.application.port.UserProviderRepositoryPort;
import com.book.core.user.application.port.UserRepositoryPort;
import com.book.core.user.application.result.IdentifyUserResult;
import com.book.core.user.domain.User;
import com.book.core.user.domain.UserProvider;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IdentifyUserUseCase {
    private static final int MAX_NICKNAME_REGISTRATION_ATTEMPTS = 5;

    private final UserRepositoryPort userRepository;
    private final UserProviderRepositoryPort userProviderRepository;
    private final NicknameGenerator nicknameGenerator;
    private final UserRegistrationUseCase userRegistrationUseCase;

    public IdentifyUserResult execute(final IdentifyUserCommand command) {
        final Optional<UserProvider> existingProvider =
                userProviderRepository.findActiveByProviderTypeAndProviderUserId(
                        command.providerType(), command.providerUserId());
        if (existingProvider.isPresent()) {
            return findExistingUser(existingProvider.orElseThrow());
        }
        return registerNewUser(command);
    }

    private IdentifyUserResult findExistingUser(final UserProvider userProvider) {
        final User user = userRepository
                .findById(userProvider.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_PROVIDER_USER_NOT_FOUND));
        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.USER_PROVIDER_USER_INACTIVE);
        }
        return new IdentifyUserResult(user.id());
    }

    private IdentifyUserResult registerNewUser(final IdentifyUserCommand command) {
        for (int attempt = 1; attempt <= MAX_NICKNAME_REGISTRATION_ATTEMPTS; attempt++) {
            final String nickname = nicknameGenerator.generate();
            final UserRegistrationCommand registrationCommand = new UserRegistrationCommand(
                    command.providerType(), command.providerUserId(), command.providerEmail(), nickname);
            try {
                return new IdentifyUserResult(userRegistrationUseCase.execute(registrationCommand));
            } catch (final BusinessException exception) {
                if (exception.errorCode() != ErrorCode.NICKNAME_CONFLICT) {
                    throw exception;
                }
            }
        }
        throw new BusinessException(ErrorCode.NICKNAME_GENERATION_FAILED);
    }
}
