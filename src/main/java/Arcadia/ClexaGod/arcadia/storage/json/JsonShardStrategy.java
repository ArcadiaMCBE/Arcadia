package Arcadia.ClexaGod.arcadia.storage.json;

import java.util.Locale;

public enum JsonShardStrategy {
    HASH("hash"),
    PREFIX("prefix");

    private final String id;

    JsonShardStrategy(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static JsonShardStrategy from(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        for (JsonShardStrategy strategy : values()) {
            if (strategy.id.equals(value)) {
                return strategy;
            }
        }
        return null;
    }
}
