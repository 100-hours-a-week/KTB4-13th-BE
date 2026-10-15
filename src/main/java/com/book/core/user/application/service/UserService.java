package com.book.core.user.application.service;

import com.book.core.user.application.result.GetMyInfoResult;
import com.book.core.user.application.usecase.GetMyInfoUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final GetMyInfoUseCase getMyInfoUseCase;

    public GetMyInfoResult getMyInfo(final Long userId) {
        return getMyInfoUseCase.execute(userId);
    }
}
