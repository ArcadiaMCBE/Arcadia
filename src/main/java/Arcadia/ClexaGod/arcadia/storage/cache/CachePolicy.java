package Arcadia.ClexaGod.arcadia.storage.cache;

import java.time.Duration;

public record CachePolicy(boolean enabled, boolean flushOnSave, Duration flushTimeout) {

    public CachePolicy {
        if (flushTimeout == null || flushTimeout.isZero() || flushTimeout.isNegative()) {
            flushTimeout = Duration.ofSeconds(2);
        }
    }

    public static CachePolicy defaultPolicy() {
        return new CachePolicy(true, false, Duration.ofSeconds(2));
    }

    public static CachePolicy flushOnSave(Duration timeout) {
        return new CachePolicy(true, true, timeout);
    }

    public static CachePolicy disabled() {
        return new CachePolicy(false, false, Duration.ofSeconds(2));
    }
}
