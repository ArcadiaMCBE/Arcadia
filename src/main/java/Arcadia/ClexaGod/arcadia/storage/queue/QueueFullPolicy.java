package Arcadia.ClexaGod.arcadia.storage.queue;

import java.util.Locale;

public enum QueueFullPolicy {
    DROP("drop"),
    BLOCK("block"),
    SYNC("sync");

    private final String id;

    QueueFullPolicy(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static QueueFullPolicy from(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        for (QueueFullPolicy policy : values()) {
            if (policy.id.equals(value)) {
                return policy;
            }
        }
        return null;
    }
}
