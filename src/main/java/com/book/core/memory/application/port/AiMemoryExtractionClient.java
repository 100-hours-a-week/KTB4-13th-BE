package com.book.core.memory.application.port;

import com.book.core.memory.application.command.UserMemoryExtraction;
import java.util.List;

public interface AiMemoryExtractionClient {
    List<UserMemoryExtraction> extract(final AiMemoryExtractionRequest request);
}
