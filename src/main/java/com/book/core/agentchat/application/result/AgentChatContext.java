package com.book.core.agentchat.application.result;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.util.List;

public record AgentChatContext(List<AgentObservedProduct> products, List<AgentContextCard> cards) {
    public AgentChatContext {
        products = List.copyOf(products);
        cards = List.copyOf(cards);
    }

    public AgentObservedProduct requireProduct(final String isbn) {
        final List<AgentObservedProduct> found =
            products.stream().filter((final AgentObservedProduct product) -> isbn != null && isbn.equals(product.isbn())).toList();
        if (found.size() != 1) {
            throw new CoreException(ErrorCode.AI_SERVICE_FAILURE);
        }
        return found.getFirst();
    }
}
