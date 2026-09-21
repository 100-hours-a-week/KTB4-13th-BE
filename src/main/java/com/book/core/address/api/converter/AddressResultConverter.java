package com.book.core.address.api.converter;

import com.book.core.address.api.response.AddressListResponse;
import com.book.core.address.api.response.AddressResponse;
import com.book.core.address.application.result.ListAddressResult;
import org.springframework.stereotype.Component;

@Component
public class AddressResultConverter {
    public AddressListResponse toListAddressResponse(final ListAddressResult result) {
        final var addresses =
                result.addresses().stream().map(AddressResponse::from).toList();
        return new AddressListResponse(addresses, null);
    }
}
