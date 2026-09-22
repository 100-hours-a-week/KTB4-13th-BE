package com.book.core.address.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.core.address.application.command.GetAddressesCommand;
import com.book.core.address.application.command.RegisterAddressCommand;
import com.book.core.address.application.command.SetDefaultAddressCommand;
import com.book.core.address.application.command.UpdateAddressCommand;
import com.book.core.address.application.result.GetAddressesResult;
import com.book.core.address.application.result.SetDefaultAddressResult;
import com.book.core.address.application.result.UpdateAddressResult;
import com.book.core.address.application.usecase.GetAddressesUseCase;
import com.book.core.address.application.usecase.RegisterAddressUseCase;
import com.book.core.address.application.usecase.SetDefaultAddressUseCase;
import com.book.core.address.application.usecase.UpdateAddressUseCase;
import java.util.List;
import org.junit.jupiter.api.Test;

class AddressServiceTest {
    private final RegisterAddressUseCase registerAddressUseCase = mock(RegisterAddressUseCase.class);
    private final GetAddressesUseCase getAddressesUseCase = mock(GetAddressesUseCase.class);
    private final UpdateAddressUseCase updateAddressUseCase = mock(UpdateAddressUseCase.class);
    private final SetDefaultAddressUseCase setDefaultAddressUseCase = mock(SetDefaultAddressUseCase.class);
    private final AddressService addressService = new AddressService(
            registerAddressUseCase, getAddressesUseCase, updateAddressUseCase, setDefaultAddressUseCase);

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

    @Test
    void 배송지_수정_Command를_수정_UseCase에_전달하고_결과를_반환한다() {
        final var command = mock(UpdateAddressCommand.class);
        final var result = new UpdateAddressResult(101L);
        when(updateAddressUseCase.execute(command)).thenReturn(result);

        assertThat(addressService.updateAddress(command)).isSameAs(result);

        verify(updateAddressUseCase).execute(command);
    }

    @Test
    void 기본_배송지_지정_Command를_기본_배송지_지정_UseCase에_전달하고_결과를_반환한다() {
        final var command = new SetDefaultAddressCommand(1L, 101L);
        final var result = new SetDefaultAddressResult(101L);
        when(setDefaultAddressUseCase.execute(command)).thenReturn(result);

        assertThat(addressService.setDefaultAddress(command)).isSameAs(result);

        verify(setDefaultAddressUseCase).execute(command);
    }
}
