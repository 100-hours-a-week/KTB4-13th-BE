package com.book.core.agentchat.application.port;

import com.book.core.agentchat.application.result.ChatResponseSnapshot;

public interface ChatResponseCodec {
    String encode(ChatResponseSnapshot response);

    ChatResponseSnapshot decode(String payload);
}
