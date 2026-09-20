package com.book.core.address.application.service;

import com.book.core.address.application.command.RegisterAddressCommand;
import com.book.core.address.application.usecase.RegisterAddressUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AddressService {
    private final RegisterAddressUseCase registerAddressUseCase;

    public void registerAddress(final RegisterAddressCommand command) {
        registerAddressUseCase.execute(command);
    }
}
