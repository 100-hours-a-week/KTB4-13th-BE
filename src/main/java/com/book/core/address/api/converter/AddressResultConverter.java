package com.book.core.address.api.converter;

import com.book.core.address.api.response.AddressListResponse;
import com.book.core.address.api.response.AddressResponse;
import com.book.core.address.api.response.UpdateAddressResponse;
import com.book.core.address.application.result.GetAddressesResult;
import com.book.core.address.application.result.UpdateAddressResult;
import org.springframework.stereotype.Component;

@Component
public class AddressResultConverter {
    public AddressListResponse toGetAddressesResponse(final GetAddressesResult result) {
        final var addresses =
                result.addresses().stream().map(AddressResponse::from).toList();
        return new AddressListResponse(addresses, null);
    }

    public UpdateAddressResponse toUpdateAddressResponse(final UpdateAddressResult result) {
        return new UpdateAddressResponse(result.addressId());
    }
}
