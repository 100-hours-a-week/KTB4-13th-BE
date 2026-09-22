package com.book.core.address.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.address.application.command.SetDefaultAddressCommand;
import com.book.core.address.application.port.AddressRepositoryPort;
import com.book.core.address.application.result.SetDefaultAddressResult;
import com.book.core.address.domain.Address;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class SetDefaultAddressUseCase {
    private final AddressRepositoryPort addressRepository;

    @Transactional
    public SetDefaultAddressResult execute(final SetDefaultAddressCommand command) {
        final Address target = addressRepository
                .findActiveByIdAndUserId(command.addressId(), command.userId())
                .orElseThrow(() -> new CoreException(ErrorCode.FORBIDDEN));

        if (!target.isDefaultAddress()) {
            final Address currentDefaultAddress = addressRepository
                    .findActiveDefaultByUserId(command.userId())
                    .orElse(null);
            target.setDefaultAddress();
            if (currentDefaultAddress != null) {
                currentDefaultAddress.unsetDefaultAddress();
            }
            addressRepository.save(target);
        }

        return new SetDefaultAddressResult(target.id());
    }
}
