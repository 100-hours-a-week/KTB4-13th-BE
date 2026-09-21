package com.book.core.address.api.request;

import java.util.Set;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

public final class UpdateAddressRequestDeserializer extends ValueDeserializer<UpdateAddressRequest> {
    @Override
    public UpdateAddressRequest deserialize(final JsonParser parser, final DeserializationContext context)
            throws JacksonException {
        final JsonNode request = context.readTree(parser);
        if (request == null || !request.isObject()) {
            return context.reportInputMismatch(UpdateAddressRequest.class, "배송지 수정 요청은 JSON 객체여야 합니다.");
        }

        return new UpdateAddressRequest(
                textValue(request, "label"),
                textValue(request, "postalCode"),
                textValue(request, "address"),
                textValue(request, "detailAddress"),
                booleanValue(request, "isDefaultAddress"),
                providedFields(request));
    }

    private static String textValue(final JsonNode request, final String fieldName) {
        final JsonNode value = request.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asString();
    }

    private static Boolean booleanValue(final JsonNode request, final String fieldName) {
        final JsonNode value = request.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.booleanValue();
    }

    private static Set<String> providedFields(final JsonNode request) {
        return Set.copyOf(request.propertyNames());
    }
}
