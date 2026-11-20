package com.book.core.library.infrastructure.client.ai;

import com.book.common.config.ai.AiRestClientConfig;
import com.book.common.config.ai.AiServerProperties;
import com.book.core.library.application.port.ShelfScanClient;

public final class ShelfScanTestClient {
    private ShelfScanTestClient() {}

    public static ShelfScanClient create(final String baseUrl) {
        return new ShelfScanClientImpl(new AiRestClientConfig().shelfScanAiRestClient(new AiServerProperties(baseUrl, "service-token")));
    }
}
