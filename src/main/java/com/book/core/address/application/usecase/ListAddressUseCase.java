package com.book.core.address.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.address.application.command.ListAddressCommand;
import com.book.core.address.application.port.AddressRepositoryPort;
import com.book.core.address.application.result.ListAddressItemResult;
import com.book.core.address.application.result.ListAddressResult;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ListAddressUseCase {
    private final AddressRepositoryPort addressRepository;

    @Transactional(readOnly = true)
    public ListAddressResult execute(final ListAddressCommand command) {
        final var addresses = addressRepository.findActiveByUserId(command.userId()).stream()
                .map((final var address) -> new ListAddressItemResult(
                        address.id(),
                        address.label(),
                        address.postalCode(),
                        address.address(),
                        address.detailAddress(),
                        address.isDefaultAddress()))
                .toList();
        return new ListAddressResult(addresses);
    }
}
