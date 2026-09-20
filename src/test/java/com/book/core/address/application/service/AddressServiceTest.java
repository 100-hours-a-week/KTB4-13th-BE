package com.book.core.address.application.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.book.core.address.application.command.RegisterAddressCommand;
import com.book.core.address.application.usecase.RegisterAddressUseCase;
import org.junit.jupiter.api.Test;

class AddressServiceTest {
    private final RegisterAddressUseCase registerAddressUseCase = mock(RegisterAddressUseCase.class);
    private final AddressService addressService = new AddressService(registerAddressUseCase);

    @Test
    void 배송지_등록_Command를_등록_UseCase에_전달한다() {
        final var command = new RegisterAddressCommand(1L, "집", "12345", "서울시 강남구", null, false);

        addressService.registerAddress(command);

        verify(registerAddressUseCase).execute(command);
    }
}
