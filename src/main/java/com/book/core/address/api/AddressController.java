package com.book.core.address.api;

import com.book.common.response.ApiResponse;
import com.book.core.address.api.converter.AddressCommandConverter;
import com.book.core.address.api.converter.AddressResultConverter;
import com.book.core.address.api.request.RegisterAddressRequest;
import com.book.core.address.api.request.UpdateAddressRequest;
import com.book.core.address.api.response.AddressListResponse;
import com.book.core.address.api.response.UpdateAddressResponse;
import com.book.core.address.api.spec.AddressControllerSpec;
import com.book.core.address.application.command.GetAddressesCommand;
import com.book.core.address.application.service.AddressService;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
            @RequestParam("userId") final Long userId, @Valid @RequestBody final RegisterAddressRequest request) {
        final var command = commandConverter.toRegisterAddressCommand(userId, request);
        addressService.registerAddress(command);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<AddressListResponse>> getAddresses(final Principal principal) {
        final GetAddressesCommand command = commandConverter.toGetAddressesCommand(principal);
        final var response = resultConverter.toGetAddressesResponse(addressService.getAddresses(command));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    @PatchMapping("/{addressId}")
    public ResponseEntity<ApiResponse<UpdateAddressResponse>> updateAddress(
            @RequestParam("userId") final Long userId,
            @PathVariable("addressId") final Long addressId,
            @RequestBody final UpdateAddressRequest request) {
        final var command = commandConverter.toUpdateAddressCommand(userId, addressId, request);
        final var response = resultConverter.toUpdateAddressResponse(addressService.updateAddress(command));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
