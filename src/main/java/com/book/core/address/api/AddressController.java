package com.book.core.address.api;

import com.book.common.response.ApiResponse;
import com.book.core.address.api.converter.AddressCommandConverter;
import com.book.core.address.api.converter.AddressResultConverter;
import com.book.core.address.api.request.RegisterAddressRequest;
import com.book.core.address.api.response.AddressListResponse;
import com.book.core.address.api.spec.AddressControllerSpec;
import com.book.core.address.application.command.ListAddressCommand;
import com.book.core.address.application.service.AddressService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user-addresses")
class AddressController implements AddressControllerSpec {
    private final AddressService addressService;
    private final AddressCommandConverter commandConverter;
    private final AddressResultConverter resultConverter;

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> registerAddress(
            @Positive @RequestParam("userId") final Long userId,
            @Valid @RequestBody final RegisterAddressRequest request) {
        final var command = commandConverter.toRegisterAddressCommand(userId, request);
        addressService.registerAddress(command);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<AddressListResponse>> listAddresses(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) final String authorization,
            final Principal principal) {
        final ListAddressCommand command = commandConverter.toListAddressCommand(authorization, principal);
        final var response = resultConverter.toListAddressResponse(addressService.listAddresses(command));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
