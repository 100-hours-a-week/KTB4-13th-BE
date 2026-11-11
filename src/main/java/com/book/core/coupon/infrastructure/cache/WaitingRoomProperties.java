package com.book.core.coupon.infrastructure.cache;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "coupon.waiting-room")
public record WaitingRoomProperties(Map<Long, Room> rooms, @DefaultValue("5") int replenishRate, @DefaultValue("10") int burstCapacity) {

    public WaitingRoomProperties {
        rooms = rooms == null ? Map.of() : Map.copyOf(rooms);
        if (replenishRate < 1 || burstCapacity < 1 || rooms.keySet().stream().anyMatch(id -> id < 1)) {
            throw new IllegalArgumentException("대기실 설정은 양수여야 합니다.");
        }
    }

    public record Room(@DefaultValue("100") int admitPerSecond, @DefaultValue("30000") long passTtlMs) {
        public Room {
            if (admitPerSecond < 1 || passTtlMs < 1) {
                throw new IllegalArgumentException("입장률과 입장권 만료 시간은 양수여야 합니다.");
            }
        }
    }
}
