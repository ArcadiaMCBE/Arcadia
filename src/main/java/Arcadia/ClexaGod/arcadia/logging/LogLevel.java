package Arcadia.ClexaGod.arcadia.logging;

import java.util.Locale;

public enum LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR;

    public static LogLevel from(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "debug" -> DEBUG;
            case "info" -> INFO;
            case "warn", "warning" -> WARN;
            case "error" -> ERROR;
            default -> null;
        };
    }
}
