package com.book.core.payment.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.payment.application.port.PgOperationRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 미확정 PG 작업이 남은 주문의 취소를 막는다.
 *
 * <p>
 * PG 응답을 받지 못한 작업이 재시도와 조회로 복구될 수 있도록, 주문 잠금이 유지된 트랜잭션에서 취소 전에 호출한다.
 */
@UseCase
@RequiredArgsConstructor
public class AssertNoPendingPgOperationUseCase {
    private final PgOperationRepositoryPort operations;

    @Transactional(propagation = Propagation.MANDATORY)
    public void execute(final String orderKey) {
        if (operations.existsPending(orderKey)) {
            throw new CoreException(ErrorCode.PG_PENDING);
        }
    }
}
