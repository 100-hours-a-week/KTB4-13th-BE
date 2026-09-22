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
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
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
    public ResponseEntity<ApiResponse<AddressListResponse>> getAddresses(
            @Positive @RequestParam("userId") final Long userId) {
        final GetAddressesCommand command = commandConverter.toGetAddressesCommand(userId);
        final var response = resultConverter.toGetAddressesResponse(addressService.getAddresses(command));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    @PutMapping("/{addressId}")
    public ResponseEntity<ApiResponse<UpdateAddressResponse>> updateAddress(
            @Positive @RequestParam("userId") final Long userId,
            @Positive @PathVariable("addressId") final Long addressId,
            @Valid @RequestBody final UpdateAddressRequest request) {
        final var command = commandConverter.toUpdateAddressCommand(userId, addressId, request);
        final var result = addressService.updateAddress(command);
        final var response = resultConverter.toUpdateAddressResponse(result);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    @PutMapping("/{addressId}/default")
    public ResponseEntity<ApiResponse<UpdateAddressResponse>> setDefaultAddress(
            @Positive @RequestParam("userId") final Long userId,
            @Positive @PathVariable("addressId") final Long addressId) {
        final var command = commandConverter.toSetDefaultAddressCommand(userId, addressId);
        final var result = addressService.setDefaultAddress(command);
        final var response = resultConverter.toSetDefaultAddressResponse(result);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @Positive @RequestParam("userId") final Long userId,
            @Positive @PathVariable("addressId") final Long addressId) {
        final var command = commandConverter.toDeleteAddressCommand(userId, addressId);
        addressService.deleteAddress(command);
        return ResponseEntity.ok().build();
    }
}
