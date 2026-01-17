package Arcadia.ClexaGod.arcadia.storage.cache;

import java.time.Duration;

public record CachePolicy(boolean flushOnSave, Duration flushTimeout) {

    public CachePolicy {
        if (flushTimeout == null || flushTimeout.isZero() || flushTimeout.isNegative()) {
            flushTimeout = Duration.ofSeconds(2);
        }
    }

    public static CachePolicy defaultPolicy() {
        return new CachePolicy(false, Duration.ofSeconds(2));
    }

    public static CachePolicy flushOnSave(Duration timeout) {
        return new CachePolicy(true, timeout);
    }
}
