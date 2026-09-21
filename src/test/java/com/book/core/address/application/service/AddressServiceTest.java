package com.book.core.address.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.core.address.application.command.ListAddressCommand;
import com.book.core.address.application.command.RegisterAddressCommand;
import com.book.core.address.application.result.ListAddressResult;
import com.book.core.address.application.usecase.ListAddressUseCase;
import com.book.core.address.application.usecase.RegisterAddressUseCase;
import java.util.List;
import org.junit.jupiter.api.Test;

class AddressServiceTest {
    private final RegisterAddressUseCase registerAddressUseCase = mock(RegisterAddressUseCase.class);
    private final ListAddressUseCase listAddressUseCase = mock(ListAddressUseCase.class);
    private final AddressService addressService = new AddressService(registerAddressUseCase, listAddressUseCase);

    @Test
    void 배송지_등록_Command를_등록_UseCase에_전달한다() {
        final var command = new RegisterAddressCommand(1L, "집", "12345", "서울시 강남구", null, false);

        addressService.registerAddress(command);

        verify(registerAddressUseCase).execute(command);
    }

    @Test
    void 배송지_목록_조회_Command를_목록_조회_UseCase에_전달한다() {
        final var command = new ListAddressCommand(1L);
        final var result = new ListAddressResult(List.of());
        when(listAddressUseCase.execute(command)).thenReturn(result);

        assertThat(addressService.listAddresses(command)).isSameAs(result);

        verify(listAddressUseCase).execute(command);
    }
}
