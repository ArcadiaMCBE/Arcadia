package Arcadia.ClexaGod.arcadia.logging;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public final class LogConfig {

    private final LogLevel defaultLevel;
    private final boolean useCoreDebug;
    private final boolean includeCategoryPrefix;
    private final Map<LogCategory, LogLevel> categoryLevels;

    public LogLevel resolveLevel(LogCategory category, boolean coreDebug) {
        if (categoryLevels != null && categoryLevels.containsKey(category)) {
            return categoryLevels.get(category);
        }
        if (useCoreDebug && coreDebug) {
            return LogLevel.DEBUG;
        }
        return defaultLevel != null ? defaultLevel : LogLevel.INFO;
    }

    public boolean hasOverride(LogCategory category) {
        return categoryLevels != null && categoryLevels.containsKey(category);
    }
}
