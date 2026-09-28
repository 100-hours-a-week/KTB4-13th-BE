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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public ResponseEntity<ApiResponse<Void>> registerAddress(@AuthenticationPrincipal final Jwt jwt,
        @Valid @RequestBody final RegisterAddressRequest request) {
        final Long userId = Long.parseLong(jwt.getSubject());
        final var command = commandConverter.toRegisterAddressCommand(userId, request);
        addressService.registerAddress(command);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<AddressListResponse>> getAddresses(@AuthenticationPrincipal final Jwt jwt) {
        final Long userId = Long.parseLong(jwt.getSubject());
        final GetAddressesCommand command = commandConverter.toGetAddressesCommand(userId);
        final var response = resultConverter.toGetAddressesResponse(addressService.getAddresses(command));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    @PutMapping("/{addressId}")
    public ResponseEntity<ApiResponse<UpdateAddressResponse>> updateAddress(@AuthenticationPrincipal final Jwt jwt,
        @Positive @PathVariable("addressId") final Long addressId, @Valid @RequestBody final UpdateAddressRequest request) {
        final Long userId = Long.parseLong(jwt.getSubject());
        final var command = commandConverter.toUpdateAddressCommand(userId, addressId, request);
        final var result = addressService.updateAddress(command);
        final var response = resultConverter.toUpdateAddressResponse(result);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    @PutMapping("/{addressId}/default")
    public ResponseEntity<ApiResponse<UpdateAddressResponse>> setDefaultAddress(@AuthenticationPrincipal final Jwt jwt,
        @Positive @PathVariable("addressId") final Long addressId) {
        final Long userId = Long.parseLong(jwt.getSubject());
        final var command = commandConverter.toSetDefaultAddressCommand(userId, addressId);
        final var result = addressService.setDefaultAddress(command);
        final var response = resultConverter.toSetDefaultAddressResponse(result);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(@AuthenticationPrincipal final Jwt jwt,
        @Positive @PathVariable("addressId") final Long addressId) {
        final Long userId = Long.parseLong(jwt.getSubject());
        final var command = commandConverter.toDeleteAddressCommand(userId, addressId);
        addressService.deleteAddress(command);
        return ResponseEntity.ok().build();
    }
}
