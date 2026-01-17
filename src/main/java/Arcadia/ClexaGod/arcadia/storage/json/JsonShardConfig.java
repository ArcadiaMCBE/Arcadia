package Arcadia.ClexaGod.arcadia.storage.json;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class JsonShardConfig {

    private final boolean enabled;
    private final JsonShardStrategy strategy;
    private final int depth;
    private final int charsPerLevel;
    private final boolean migrateLegacyOnRead;

    public static JsonShardConfig disabled() {
        return new JsonShardConfig(false, JsonShardStrategy.HASH, 2, 2, false);
    }
}
