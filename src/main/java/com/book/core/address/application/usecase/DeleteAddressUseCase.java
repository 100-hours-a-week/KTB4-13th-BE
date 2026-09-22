package com.book.core.address.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.address.application.command.DeleteAddressCommand;
import com.book.core.address.application.port.AddressRepositoryPort;
import com.book.core.address.domain.Address;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class DeleteAddressUseCase {
    private final AddressRepositoryPort addressRepository;

    @Transactional
    public void execute(final DeleteAddressCommand command) {
        final Address target = addressRepository
                .findActiveByIdAndUserId(command.addressId(), command.userId())
                .orElseThrow(() -> new CoreException(ErrorCode.FORBIDDEN));

        final Address latestAddress;
        if (target.isDefaultAddress()) {
            latestAddress = addressRepository
                    .findLatestActiveByUserIdExcludingId(command.userId(), command.addressId())
                    .orElse(null);
        } else {
            latestAddress = null;
        }

        target.unsetDefaultAddress();
        target.delete();
        if (latestAddress != null) {
            latestAddress.setDefaultAddress();
        }
        addressRepository.save(target);
    }
}
