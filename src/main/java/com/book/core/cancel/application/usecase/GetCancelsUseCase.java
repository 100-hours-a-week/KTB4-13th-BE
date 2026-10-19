package com.book.core.cancel.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.cancel.application.port.CancelRepositoryPort;
import com.book.core.cancel.domain.Cancel;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetCancelsUseCase {
    private final CancelRepositoryPort cancelRepository;

    /** 주문의 취소 기록을 canceledAt DESC, id DESC 순서로 조회합니다. */
    @Transactional(readOnly = true)
    public List<Cancel> execute(final Long orderId) {
        return cancelRepository.findActiveByOrderIdLatestFirst(orderId);
    }
}
