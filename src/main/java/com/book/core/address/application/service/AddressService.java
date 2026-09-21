package com.book.core.address.application.service;

import com.book.core.address.application.command.GetAddressesCommand;
import com.book.core.address.application.command.RegisterAddressCommand;
import com.book.core.address.application.command.UpdateAddressCommand;
import com.book.core.address.application.result.GetAddressesResult;
import com.book.core.address.application.result.UpdateAddressResult;
import com.book.core.address.application.usecase.GetAddressesUseCase;
import com.book.core.address.application.usecase.RegisterAddressUseCase;
import com.book.core.address.application.usecase.UpdateAddressUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AddressService {
    private final RegisterAddressUseCase registerAddressUseCase;
    private final GetAddressesUseCase getAddressesUseCase;
    private final UpdateAddressUseCase updateAddressUseCase;

    public void registerAddress(final RegisterAddressCommand command) {
        registerAddressUseCase.execute(command);
    }

    public GetAddressesResult getAddresses(final GetAddressesCommand command) {
        return getAddressesUseCase.execute(command);
    }

    public UpdateAddressResult updateAddress(final UpdateAddressCommand command) {
        return updateAddressUseCase.execute(command);
    }
}
