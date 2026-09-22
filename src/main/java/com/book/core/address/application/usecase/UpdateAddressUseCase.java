package com.book.core.address.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.address.application.command.UpdateAddressCommand;
import com.book.core.address.application.port.AddressRepositoryPort;
import com.book.core.address.application.result.UpdateAddressResult;
import com.book.core.address.domain.Address;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class UpdateAddressUseCase {
    private final AddressRepositoryPort addressRepository;

    @Transactional
    public UpdateAddressResult execute(final UpdateAddressCommand command) {
        final Address address = addressRepository
                .findActiveByIdAndUserId(command.addressId(), command.userId())
                .orElseThrow(() -> new CoreException(ErrorCode.FORBIDDEN));
        final boolean isCurrentDefaultAddress = address.isDefaultAddress();
        final Address updatedAddress = new Address(
                address.id(),
                address.userId(),
                command.label(),
                command.postalCode(),
                command.address(),
                command.detailAddress(),
                isCurrentDefaultAddress);

        if (addressRepository.existsActiveDuplicateAddress(command.userId(), updatedAddress)) {
            throw new CoreException(ErrorCode.DUPLICATE_ADDRESS);
        }

        address.updateDetailsFrom(updatedAddress);
        final Address savedAddress = addressRepository.save(address);
        return new UpdateAddressResult(savedAddress.id());
    }
}
