package com.book.core.address.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.address.application.port.AddressRepositoryPort;
import com.book.core.address.application.result.GetAddressItemResult;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetAddressUseCase {
    private final AddressRepositoryPort addressRepository;

    @Transactional(readOnly = true)
    public Optional<GetAddressItemResult> execute(final Long userId) {
        if (userId == null || userId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        return addressRepository.findActiveDefaultByUserId(userId).map(GetAddressItemResult::from);
    }
}
