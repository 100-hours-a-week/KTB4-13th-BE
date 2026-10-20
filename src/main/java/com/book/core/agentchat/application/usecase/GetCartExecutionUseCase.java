package com.book.core.agentchat.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.agentchat.application.port.CartChangeRepositoryPort;
import com.book.core.agentchat.application.result.CartChangeResult;
import com.book.core.agentchat.domain.CartChangeRequest;
import com.book.core.agentchat.domain.CartExecution;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetCartExecutionUseCase {
    private final CartChangeRepositoryPort changes;

    @Transactional(readOnly = true)
    public CartChangeResult execute(final Long userId, final Long executionId) {
        final CartExecution execution =
            changes.findExecution(userId, executionId).orElseThrow(() -> new CoreException(ErrorCode.CART_CHANGE_NOT_FOUND));
        // 과거 성공은 불변 요청 항목을 읽으며 Cart 잠금을 요구하지 않습니다.
        final CartChangeRequest request =
            changes.findRequest(userId, execution.requestId()).orElseThrow(() -> new IllegalStateException("성공 실행의 원본 요청이 없습니다."));
        return new CartChangeResult(execution.id(), execution.proposalId(), request.items(), execution.appliedAt(), null, true);
    }
}
