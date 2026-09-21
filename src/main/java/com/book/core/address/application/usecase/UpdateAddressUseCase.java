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
        final boolean currentDefaultAddress = address.isDefaultAddress();
        final Address updatedAddress = new Address(
                address.id(),
                address.userId(),
                command.label().valueOrElse(address.label()),
                command.postalCode().valueOrElse(address.postalCode()),
                command.address().valueOrElse(address.address()),
                command.detailAddress().valueOrElse(address.detailAddress()),
                command.defaultAddress().valueOrElse(currentDefaultAddress));

        if (currentDefaultAddress && !updatedAddress.isDefaultAddress()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (addressRepository.existsActiveDuplicateAddress(command.userId(), updatedAddress)) {
            throw new CoreException(ErrorCode.DUPLICATE_ADDRESS);
        }

        Address previousDefaultAddress = null;
        if (updatedAddress.isDefaultAddress() && !currentDefaultAddress) {
            previousDefaultAddress = addressRepository
                    .findActiveDefaultByUserId(command.userId())
                    .orElse(null);
        }

        address.updateFrom(updatedAddress);
        final Address savedAddress = addressRepository.save(address);
        if (previousDefaultAddress != null) {
            previousDefaultAddress.unsetDefaultAddress();
        }
        return new UpdateAddressResult(savedAddress.id());
    }
}
