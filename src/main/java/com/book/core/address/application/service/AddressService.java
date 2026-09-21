package com.book.core.address.application.service;

import com.book.core.address.application.command.ListAddressCommand;
import com.book.core.address.application.command.RegisterAddressCommand;
import com.book.core.address.application.result.ListAddressResult;
import com.book.core.address.application.usecase.ListAddressUseCase;
import com.book.core.address.application.usecase.RegisterAddressUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AddressService {
    private final RegisterAddressUseCase registerAddressUseCase;
    private final ListAddressUseCase listAddressUseCase;

    public void registerAddress(final RegisterAddressCommand command) {
        registerAddressUseCase.execute(command);
    }

    public ListAddressResult listAddresses(final ListAddressCommand command) {
        return listAddressUseCase.execute(command);
    }
}
