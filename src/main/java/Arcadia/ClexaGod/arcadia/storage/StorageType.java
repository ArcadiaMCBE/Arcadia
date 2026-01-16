package Arcadia.ClexaGod.arcadia.storage;

import java.util.Locale;

public enum StorageType {
    JSON("json"),
    POSTGRESQL("postgresql");

    private final String id;

    StorageType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static StorageType from(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (StorageType type : values()) {
            if (type.id.equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}
