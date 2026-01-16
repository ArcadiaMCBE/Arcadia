package Arcadia.ClexaGod.arcadia.storage.cache;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class CacheConfig {

    private final boolean enabled;
    private final int ttlSeconds;
    private final int maxEntries;
    private final int flushIntervalSeconds;
}
