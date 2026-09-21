package com.book.core.address.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class UpdateAddressRequest {
    private String label;
    private boolean labelProvided;
    private String postalCode;
    private boolean postalCodeProvided;
    private String address;
    private boolean addressProvided;
    private String detailAddress;
    private boolean detailAddressProvided;
    private Boolean defaultAddress;
    private boolean defaultAddressProvided;

    @JsonProperty("label")
    public void setLabel(final String label) {
        this.labelProvided = true;
        this.label = label;
    }

    @JsonProperty("postalCode")
    public void setPostalCode(final String postalCode) {
        this.postalCodeProvided = true;
        this.postalCode = postalCode;
    }

    @JsonProperty("address")
    public void setAddress(final String address) {
        this.addressProvided = true;
        this.address = address;
    }

    @JsonProperty("detailAddress")
    public void setDetailAddress(final String detailAddress) {
        this.detailAddressProvided = true;
        this.detailAddress = detailAddress;
    }

    @JsonProperty("isDefaultAddress")
    public void setDefaultAddress(final Boolean defaultAddress) {
        this.defaultAddressProvided = true;
        this.defaultAddress = defaultAddress;
    }

    public String label() {
        return label;
    }

    public boolean hasLabel() {
        return labelProvided;
    }

    public String postalCode() {
        return postalCode;
    }

    public boolean hasPostalCode() {
        return postalCodeProvided;
    }

    public String address() {
        return address;
    }

    public boolean hasAddress() {
        return addressProvided;
    }

    public String detailAddress() {
        return detailAddress;
    }

    public boolean hasDetailAddress() {
        return detailAddressProvided;
    }

    public Boolean defaultAddress() {
        return defaultAddress;
    }

    public boolean hasDefaultAddress() {
        return defaultAddressProvided;
    }
}
