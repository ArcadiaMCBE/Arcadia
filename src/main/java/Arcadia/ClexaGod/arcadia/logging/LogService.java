package Arcadia.ClexaGod.arcadia.logging;

import org.slf4j.Logger;

import java.util.Objects;

public final class LogService {

    private final Logger logger;
    private volatile LogConfig config;
    private volatile boolean coreDebug;

    public LogService(Logger logger, LogConfig config, boolean coreDebug) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.config = config;
        this.coreDebug = coreDebug;
    }

    public void update(LogConfig config, boolean coreDebug) {
        this.config = config;
        this.coreDebug = coreDebug;
    }

    public void debug(LogCategory category, String message) {
        log(category, LogLevel.DEBUG, message, null);
    }

    public void info(LogCategory category, String message) {
        log(category, LogLevel.INFO, message, null);
    }

    public void warn(LogCategory category, String message) {
        log(category, LogLevel.WARN, message, null);
    }

    public void error(LogCategory category, String message) {
        log(category, LogLevel.ERROR, message, null);
    }

    public void error(LogCategory category, String message, Throwable throwable) {
        log(category, LogLevel.ERROR, message, throwable);
    }

    public void warn(LogCategory category, String message, Throwable throwable) {
        log(category, LogLevel.WARN, message, throwable);
    }

    private void log(LogCategory category, LogLevel level, String message, Throwable throwable) {
        if (message == null || level == null) {
            return;
        }
        LogConfig current = this.config;
        LogLevel minLevel = current != null ? current.resolveLevel(category, coreDebug) : LogLevel.INFO;
        if (level.ordinal() < minLevel.ordinal()) {
            return;
        }
        String rendered = withPrefix(current, category, message);
        switch (level) {
            case DEBUG -> logger.debug(rendered, throwable);
            case INFO -> logger.info(rendered, throwable);
            case WARN -> logger.warn(rendered, throwable);
            case ERROR -> logger.error(rendered, throwable);
        }
    }

    private String withPrefix(LogConfig config, LogCategory category, String message) {
        if (config == null || category == null || !config.isIncludeCategoryPrefix()) {
            return message;
        }
        return "[" + category.getId() + "] " + message;
    }
}
