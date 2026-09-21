package com.book.core.user.infrastructure.nickname;

import com.book.core.user.application.port.NicknameGenerator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

@Component
public final class RandomNicknameGenerator implements NicknameGenerator {
    private static final List<String> ADJECTIVES =
            List.of("고요한", "다정한", "따뜻한", "맑은", "반가운", "빛나는", "신나는", "씩씩한", "용감한", "즐거운", "총명한", "푸른", "포근한", "행복한");
    private static final List<String> NOUNS = List.of(
            "고양이", "구름", "나무", "노을", "다람쥐", "달빛", "도서관", "독자", "바람", "별빛", "부엉이", "새벽", "여우", "연필", "책갈피", "책벌레", "참새",
            "토끼");
    private static final int MIN_SUFFIX = 1000;
    private static final int MAX_SUFFIX_EXCLUSIVE = 10000;

    @Override
    public String generate() {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final String adjective = ADJECTIVES.get(random.nextInt(ADJECTIVES.size()));
        final String noun = NOUNS.get(random.nextInt(NOUNS.size()));
        final int suffix = random.nextInt(MIN_SUFFIX, MAX_SUFFIX_EXCLUSIVE);
        return adjective + noun + suffix;
    }
}
