package com.book.core.address.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.address.application.command.RegisterAddressCommand;
import com.book.core.address.application.port.AddressRepositoryPort;
import com.book.core.address.domain.Address;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class RegisterAddressUseCase {
    private static final int MAX_ACTIVE_ADDRESS_COUNT = 3;

    private final AddressRepositoryPort addressRepository;

    @Transactional
    public void execute(final RegisterAddressCommand command) {
        final int activeAddressCount = addressRepository.countActiveByUserId(command.userId());
        if (activeAddressCount >= MAX_ACTIVE_ADDRESS_COUNT) {
            throw new CoreException(ErrorCode.ADDRESS_LIMIT_EXCEEDED);
        }

        final Address address = Address.register(
                command.userId(), command.label(), command.postalCode(), command.address(), command.detailAddress());
        if (addressRepository.existsActiveDuplicateAddress(command.userId(), address)) {
            throw new CoreException(ErrorCode.DUPLICATE_ADDRESS);
        }

        final boolean firstAddress = activeAddressCount == 0;
        Address currentDefault = null;
        if (command.isDefaultAddress() && !firstAddress) {
            currentDefault = addressRepository
                    .findActiveDefaultByUserId(command.userId())
                    .orElse(null);
        }

        if (firstAddress || command.isDefaultAddress()) {
            address.setDefaultAddress();
        }
        addressRepository.save(address);
        if (currentDefault != null) {
            currentDefault.unsetDefaultAddress();
        }
    }
}
