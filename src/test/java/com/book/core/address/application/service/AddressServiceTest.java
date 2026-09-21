package com.book.core.address.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.core.address.application.command.GetAddressesCommand;
import com.book.core.address.application.command.RegisterAddressCommand;
import com.book.core.address.application.result.GetAddressesResult;
import com.book.core.address.application.usecase.GetAddressesUseCase;
import com.book.core.address.application.usecase.RegisterAddressUseCase;
import java.util.List;
import org.junit.jupiter.api.Test;

class AddressServiceTest {
    private final RegisterAddressUseCase registerAddressUseCase = mock(RegisterAddressUseCase.class);
    private final GetAddressesUseCase getAddressesUseCase = mock(GetAddressesUseCase.class);
    private final AddressService addressService = new AddressService(registerAddressUseCase, getAddressesUseCase);

    @Test
    void 배송지_등록_Command를_등록_UseCase에_전달한다() {
        final var command = new RegisterAddressCommand(1L, "집", "12345", "서울시 강남구", null, false);

        addressService.registerAddress(command);

        verify(registerAddressUseCase).execute(command);
    }

    @Test
    void 배송지_목록_조회_Command를_목록_조회_UseCase에_전달한다() {
        final var command = new GetAddressesCommand(1L);
        final var result = new GetAddressesResult(List.of());
        when(getAddressesUseCase.execute(command)).thenReturn(result);

        assertThat(addressService.getAddresses(command)).isSameAs(result);

        verify(getAddressesUseCase).execute(command);
    }
}
