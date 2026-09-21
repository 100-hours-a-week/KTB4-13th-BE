package com.book.core.address.api.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Set;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = UpdateAddressRequestDeserializer.class)
public record UpdateAddressRequest(
        String label,
        String postalCode,
        String address,
        String detailAddress,
        Boolean isDefaultAddress,
        @JsonIgnore Set<String> providedFields) {

    public UpdateAddressRequest {
        providedFields = Set.copyOf(providedFields);
    }

    public boolean hasLabel() {
        return providedFields.contains("label");
    }

    public boolean hasPostalCode() {
        return providedFields.contains("postalCode");
    }

    public boolean hasAddress() {
        return providedFields.contains("address");
    }

    public boolean hasDetailAddress() {
        return providedFields.contains("detailAddress");
    }

    public boolean hasDefaultAddress() {
        return providedFields.contains("isDefaultAddress");
    }
}
