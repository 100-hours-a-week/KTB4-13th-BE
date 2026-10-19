package com.book.core.user.application.service;

import com.book.core.user.application.command.UpdateMyInfoCommand;
import com.book.core.user.application.result.GetMyInfoResult;
import com.book.core.user.application.usecase.GetMyInfoUseCase;
import com.book.core.user.application.usecase.RequestWithdrawalUseCase;
import com.book.core.user.application.usecase.UpdateMyInfoUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final GetMyInfoUseCase getMyInfoUseCase;
    private final UpdateMyInfoUseCase updateMyInfoUseCase;
    private final RequestWithdrawalUseCase requestWithdrawalUseCase;

    public GetMyInfoResult getMyInfo(final Long userId) {
        return getMyInfoUseCase.execute(userId);
    }

    public void updateMyInfo(final UpdateMyInfoCommand command) {
        updateMyInfoUseCase.execute(command);
    }

    public void requestWithdrawal(final Long userId) {
        requestWithdrawalUseCase.execute(userId);
    }
}
