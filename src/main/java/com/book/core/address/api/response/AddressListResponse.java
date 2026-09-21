package com.book.core.address.api.response;

import java.util.List;

public record AddressListResponse(List<AddressResponse> addresses, String nextCursor) {}
