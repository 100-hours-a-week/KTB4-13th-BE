package com.book.core.agentchat.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class JacksonChatResponseCodecTest {
    @Test
    void 새_형식_도입_전_JSON도_기존_문장과_문자열_버튼으로_복원한다() {
        final JacksonChatResponseCodec codec = new JacksonChatResponseCodec(JsonMapper.builder().build());
        final String oldPayload = """
            {"schemaVersion":1,"spec":{"intent":"semantic"},"reply":"당시 답변","cards":[],
            "followup":null,"buttons":["다른 책"],"degraded":true}
            """;
        assertThat(codec.decode(oldPayload).schemaVersion()).isEqualTo(1);
        assertThat(codec.decode(oldPayload).reply()).isEqualTo("당시 답변");
        assertThat(codec.decode(oldPayload).buttons()).containsExactly("다른 책");
        assertThat(codec.decode(oldPayload).agentButtons()).isEmpty();
    }
}
