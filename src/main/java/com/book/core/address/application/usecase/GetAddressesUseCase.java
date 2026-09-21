package com.book.core.address.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.address.application.command.GetAddressesCommand;
import com.book.core.address.application.port.AddressRepositoryPort;
import com.book.core.address.application.result.GetAddressItemResult;
import com.book.core.address.application.result.GetAddressesResult;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetAddressesUseCase {
    private final AddressRepositoryPort addressRepository;

    @Transactional(readOnly = true)
    public GetAddressesResult execute(final GetAddressesCommand command) {
        final var addresses = addressRepository.findActiveByUserId(command.userId()).stream()
                .map(GetAddressItemResult::from)
                .toList();
        return GetAddressesResult.of(addresses);
    }
}
