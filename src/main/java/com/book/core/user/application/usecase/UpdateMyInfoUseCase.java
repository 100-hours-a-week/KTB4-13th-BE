package com.book.core.user.application.usecase;

import com.book.common.exception.BusinessException;
import com.book.common.exception.ErrorCode;
import com.book.core.user.application.command.UpdateMyInfoCommand;
import com.book.core.user.application.port.UserRepositoryPort;
import com.book.core.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateMyInfoUseCase {
    private final UserRepositoryPort userRepository;

    @Transactional
    public void execute(final UpdateMyInfoCommand command) {
        final User user = userRepository.findById(command.userId()).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        user.changeNickname(command.nickname());
    }
}
