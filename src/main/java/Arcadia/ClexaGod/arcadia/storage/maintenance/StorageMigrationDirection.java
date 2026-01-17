package Arcadia.ClexaGod.arcadia.storage.maintenance;

import java.util.Locale;

public enum StorageMigrationDirection {
    JSON_TO_POSTGRES("json-to-postgres"),
    POSTGRES_TO_JSON("postgres-to-json");

    private final String id;

    StorageMigrationDirection(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static StorageMigrationDirection from(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        for (StorageMigrationDirection direction : values()) {
            if (direction.id.equals(value)) {
                return direction;
            }
        }
        return null;
    }
}
