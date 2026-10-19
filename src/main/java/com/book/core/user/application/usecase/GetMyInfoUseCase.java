package com.book.core.user.application.usecase;

import com.book.common.exception.BusinessException;
import com.book.common.exception.ErrorCode;
import com.book.core.user.application.port.UserRepositoryPort;
import com.book.core.user.application.result.GetMyInfoResult;
import com.book.core.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetMyInfoUseCase {
    private final UserRepositoryPort userRepository;

    @Transactional(readOnly = true)
    public GetMyInfoResult execute(final Long userId) {
        final User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return new GetMyInfoResult(user.nickname());
    }
}
