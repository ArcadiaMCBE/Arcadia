package Arcadia.ClexaGod.arcadia.storage.cache;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Locale;
import java.util.Map;

@Getter
@AllArgsConstructor
public final class CacheConfig {

    private final boolean enabled;
    private final int ttlSeconds;
    private final int maxEntries;
    private final int flushIntervalSeconds;
    private final boolean warmupEnabled;
    private final int warmupMaxEntries;
    private final int warmupDelaySeconds;
    private final boolean flushOnPlayerQuit;
    private final CachePolicy defaultPolicy;
    private final Map<String, CachePolicy> policies;

    public CachePolicy resolvePolicy(String repositoryName) {
        CachePolicy fallback = defaultPolicy != null ? defaultPolicy : CachePolicy.defaultPolicy();
        if (repositoryName == null || repositoryName.isBlank()) {
            return fallback;
        }
        if (policies == null || policies.isEmpty()) {
            return fallback;
        }
        String key = repositoryName.toLowerCase(Locale.ROOT);
        CachePolicy override = policies.get(key);
        return override != null ? override : fallback;
    }
}
