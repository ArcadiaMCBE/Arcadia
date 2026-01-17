package Arcadia.ClexaGod.arcadia.logging;

import java.util.Locale;

public enum LogCategory {
    CORE("core"),
    CONFIG("config"),
    MODULE("module"),
    STORAGE("storage"),
    CACHE("cache"),
    QUEUE("queue"),
    MIGRATION("migration"),
    MAINTENANCE("maintenance");

    private final String id;

    LogCategory(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static LogCategory from(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        for (LogCategory category : values()) {
            if (category.id.equals(value)) {
                return category;
            }
        }
        return null;
    }
}
