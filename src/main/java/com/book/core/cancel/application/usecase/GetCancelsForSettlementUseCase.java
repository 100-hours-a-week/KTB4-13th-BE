package com.book.core.cancel.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.paging.SliceResult;
import com.book.core.cancel.application.port.CancelRepositoryPort;
import com.book.core.cancel.domain.Cancel;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetCancelsForSettlementUseCase {
    private final CancelRepositoryPort cancelRepository;

    @Transactional(readOnly = true)
    public SliceResult<Cancel> execute(final LocalDateTime from, final LocalDateTime to, final int page, final int size) {
        return cancelRepository.findForSettlement(from, to, page, size);
    }
}
