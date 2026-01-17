package Arcadia.ClexaGod.arcadia.config;

import Arcadia.ClexaGod.arcadia.i18n.LangKeys;
import Arcadia.ClexaGod.arcadia.storage.PostgresConfig;
import Arcadia.ClexaGod.arcadia.storage.StorageType;
import Arcadia.ClexaGod.arcadia.storage.cache.CacheConfig;
import Arcadia.ClexaGod.arcadia.storage.queue.QueueFullPolicy;
import Arcadia.ClexaGod.arcadia.storage.retry.RetryPolicy;
import lombok.Getter;
import org.allaymc.api.message.I18n;
import org.allaymc.api.message.LangCode;
import org.allaymc.api.utils.config.Config;
import org.allaymc.api.utils.config.ConfigSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Getter
public final class CoreConfig {

    private final String owner;
    private final String serverName;
    private final boolean debug;
    private final String defaultLang;
    private final StorageType storageType;
    private final String storageJsonPath;
    private final int storageQueueMaxSize;
    private final QueueFullPolicy storageQueueFullPolicy;
    private final int storageQueueFullTimeoutMs;
    private final RetryPolicy storageRetryPolicy;
    private final PostgresConfig postgresConfig;
    private final CacheConfig cacheConfig;
    private final Map<String, Boolean> moduleToggles;
    private final List<ConfigIssue> issues;

    private CoreConfig(String owner, String serverName, boolean debug, String defaultLang,
                       StorageType storageType, String storageJsonPath, int storageQueueMaxSize, QueueFullPolicy storageQueueFullPolicy,
                       int storageQueueFullTimeoutMs, RetryPolicy storageRetryPolicy, PostgresConfig postgresConfig,
                       CacheConfig cacheConfig, Map<String, Boolean> moduleToggles, List<ConfigIssue> issues) {
        this.owner = owner;
        this.serverName = serverName;
        this.debug = debug;
        this.defaultLang = defaultLang;
        this.storageType = storageType;
        this.storageJsonPath = storageJsonPath;
        this.storageQueueMaxSize = storageQueueMaxSize;
        this.storageQueueFullPolicy = storageQueueFullPolicy;
        this.storageQueueFullTimeoutMs = storageQueueFullTimeoutMs;
        this.storageRetryPolicy = storageRetryPolicy;
        this.postgresConfig = postgresConfig;
        this.cacheConfig = cacheConfig;
        this.moduleToggles = Collections.unmodifiableMap(moduleToggles);
        this.issues = Collections.unmodifiableList(issues);
    }

    public LangCode getDefaultLangCode() {
        LangCode lang = LangCode.byName(defaultLang);
        return lang != null ? lang : I18n.FALLBACK_LANG;
    }

    public boolean isModuleEnabled(String moduleName) {
        if (moduleName == null || moduleName.isBlank()) {
            return false;
        }
        String key = moduleName.toLowerCase(Locale.ROOT);
        return moduleToggles.getOrDefault(key, true);
    }

    public static CoreConfig from(Config config) {
        List<ConfigIssue> issues = new ArrayList<>();

        String owner = config.getString("core.owner", "ClexaGod").trim();
        if (owner.isBlank()) {
            issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_OWNER_INVALID));
            owner = "ClexaGod";
        }

        String serverName = config.getString("core.server-name", "Arcadia").trim();
        if (serverName.isBlank()) {
            issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_SERVER_NAME_INVALID));
            serverName = "Arcadia";
        }

        boolean debug = config.getBoolean("core.debug", false);
        String defaultLangRaw = config.getString("core.default-lang", I18n.FALLBACK_LANG.name()).trim();
        LangCode langCode = LangCode.byName(defaultLangRaw);
        String defaultLang = langCode != null ? langCode.name() : I18n.FALLBACK_LANG.name();
        if (langCode == null) {
            issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_DEFAULT_LANG_INVALID, defaultLangRaw));
        }

        String storageRaw = config.getString("storage.type", "json").trim();
        StorageType storageType = StorageType.from(storageRaw);
        if (storageType == null) {
            issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_TYPE_INVALID, storageRaw));
            storageType = StorageType.JSON;
        }

        String storageJsonPath = config.getString("storage.json.path", "data").trim();
        if (storageJsonPath.isBlank()) {
            issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_JSON_PATH_INVALID));
            storageJsonPath = "data";
        }

        int storageQueueMaxSize = config.getInt("storage.queue.max-size", 5000);
        if (storageQueueMaxSize <= 0) {
            issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_QUEUE_MAX_INVALID, String.valueOf(storageQueueMaxSize)));
            storageQueueMaxSize = 5000;
        }

        String queuePolicyRaw = config.getString("storage.queue.on-full", "block").trim();
        QueueFullPolicy queuePolicy = QueueFullPolicy.from(queuePolicyRaw);
        if (queuePolicy == null) {
            issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_QUEUE_ON_FULL_INVALID, queuePolicyRaw));
            queuePolicy = QueueFullPolicy.BLOCK;
        }

        int queueFullTimeoutMs = config.getInt("storage.queue.full-timeout-ms", 200);
        if (queueFullTimeoutMs < 0) {
            issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_QUEUE_TIMEOUT_INVALID, String.valueOf(queueFullTimeoutMs)));
            queueFullTimeoutMs = 200;
        }

        boolean retryEnabled = config.getBoolean("storage.retry.enabled", true);
        int retryAttempts = config.getInt("storage.retry.max-attempts", 3);
        if (retryAttempts < 1) {
            issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_RETRY_ATTEMPTS_INVALID, String.valueOf(retryAttempts)));
            retryAttempts = 3;
        }
        int retryBaseDelayMs = config.getInt("storage.retry.base-delay-ms", 50);
        if (retryBaseDelayMs < 0) {
            issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_RETRY_BASE_DELAY_INVALID, String.valueOf(retryBaseDelayMs)));
            retryBaseDelayMs = 50;
        }
        int retryMaxDelayMs = config.getInt("storage.retry.max-delay-ms", 1000);
        if (retryMaxDelayMs < retryBaseDelayMs) {
            issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_RETRY_MAX_DELAY_INVALID, String.valueOf(retryMaxDelayMs)));
            retryMaxDelayMs = Math.max(retryBaseDelayMs, 1000);
        }
        int retryJitterMs = config.getInt("storage.retry.jitter-ms", 50);
        if (retryJitterMs < 0) {
            issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_RETRY_JITTER_INVALID, String.valueOf(retryJitterMs)));
            retryJitterMs = 50;
        }
        RetryPolicy retryPolicy = new RetryPolicy(retryEnabled, retryAttempts, retryBaseDelayMs, retryMaxDelayMs, retryJitterMs);

        String pgHost = config.getString("storage.postgresql.host", "127.0.0.1").trim();
        int pgPort = config.getInt("storage.postgresql.port", 5432);
        String pgDatabase = config.getString("storage.postgresql.database", "arcadia").trim();
        String pgUser = config.getString("storage.postgresql.user", "arcadia").trim();
        String pgPassword = config.getString("storage.postgresql.password", "").trim();
        boolean pgSsl = config.getBoolean("storage.postgresql.ssl", false);
        int pgMaxPool = config.getInt("storage.postgresql.pool.max-size", 10);
        int pgMinIdle = config.getInt("storage.postgresql.pool.min-idle", 2);
        int pgConnTimeout = config.getInt("storage.postgresql.pool.connection-timeout-ms", 10000);
        int pgIdleTimeout = config.getInt("storage.postgresql.pool.idle-timeout-ms", 600000);

        if (storageType == StorageType.POSTGRESQL) {
            if (pgHost.isBlank()) {
                issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_POSTGRES_HOST_INVALID));
                pgHost = "127.0.0.1";
            }
            if (pgPort <= 0 || pgPort > 65535) {
                issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_POSTGRES_PORT_INVALID, String.valueOf(pgPort)));
                pgPort = 5432;
            }
            if (pgDatabase.isBlank()) {
                issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_POSTGRES_DB_INVALID));
                pgDatabase = "arcadia";
            }
            if (pgUser.isBlank()) {
                issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_POSTGRES_USER_INVALID));
                pgUser = "arcadia";
            }
            if (pgMaxPool <= 0) {
                issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_POSTGRES_POOL_INVALID, "max-size", String.valueOf(pgMaxPool)));
                pgMaxPool = 10;
            }
            if (pgMinIdle < 0) {
                issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_POSTGRES_POOL_INVALID, "min-idle", String.valueOf(pgMinIdle)));
                pgMinIdle = 2;
            }
            if (pgMinIdle > pgMaxPool) {
                issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_POSTGRES_POOL_INVALID, "min-idle", String.valueOf(pgMinIdle)));
                pgMinIdle = pgMaxPool;
            }
            if (pgConnTimeout <= 0) {
                issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_POSTGRES_POOL_INVALID, "connection-timeout-ms", String.valueOf(pgConnTimeout)));
                pgConnTimeout = 10000;
            }
            if (pgIdleTimeout <= 0) {
                issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_POSTGRES_POOL_INVALID, "idle-timeout-ms", String.valueOf(pgIdleTimeout)));
                pgIdleTimeout = 600000;
            }
        }

        PostgresConfig postgresConfig = new PostgresConfig(
                pgHost, pgPort, pgDatabase, pgUser, pgPassword, pgSsl,
                pgMaxPool, pgMinIdle, pgConnTimeout, pgIdleTimeout
        );

        boolean cacheEnabled = config.getBoolean("cache.enabled", true);
        int cacheTtlSeconds = config.getInt("cache.ttl-seconds", 900);
        if (cacheTtlSeconds <= 0) {
            issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_CACHE_TTL_INVALID, String.valueOf(cacheTtlSeconds)));
            cacheTtlSeconds = 900;
        }
        int cacheMaxEntries = config.getInt("cache.max-entries", 2100);
        if (cacheMaxEntries <= 0) {
            issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_CACHE_MAX_ENTRIES_INVALID, String.valueOf(cacheMaxEntries)));
            cacheMaxEntries = 2100;
        }
        int cacheFlushInterval = config.getInt("cache.flush-interval-seconds", 10);
        if (cacheFlushInterval <= 0) {
            issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_CACHE_FLUSH_INTERVAL_INVALID, String.valueOf(cacheFlushInterval)));
            cacheFlushInterval = 10;
        }
        boolean warmupEnabled = config.getBoolean("cache.warmup.enabled", false);
        int warmupMaxEntries = config.getInt("cache.warmup.max-entries-per-repo", 500);
        if (warmupMaxEntries <= 0) {
            issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_CACHE_WARMUP_MAX_INVALID, String.valueOf(warmupMaxEntries)));
            warmupMaxEntries = 500;
        }
        int warmupDelaySeconds = config.getInt("cache.warmup.delay-seconds", 5);
        if (warmupDelaySeconds < 0) {
            issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_CACHE_WARMUP_DELAY_INVALID, String.valueOf(warmupDelaySeconds)));
            warmupDelaySeconds = 5;
        }
        boolean flushOnPlayerQuit = config.getBoolean("cache.flush-on-player-quit", true);

        CacheConfig cacheConfig = new CacheConfig(
                cacheEnabled,
                cacheTtlSeconds,
                cacheMaxEntries,
                cacheFlushInterval,
                warmupEnabled,
                warmupMaxEntries,
                warmupDelaySeconds,
                flushOnPlayerQuit
        );

        Map<String, Boolean> moduleToggles = new LinkedHashMap<>();
        ConfigSection section = config.getSection("modules");
        for (Map.Entry<String, Object> entry : section.entrySet()) {
            String key = entry.getKey().toLowerCase(Locale.ROOT);
            boolean enabled;
            Object value = entry.getValue();
            if (value instanceof Boolean bool) {
                enabled = bool;
            } else {
                enabled = Boolean.parseBoolean(String.valueOf(value));
            }
            moduleToggles.put(key, enabled);
        }

        return new CoreConfig(owner, serverName, debug, defaultLang,
                storageType, storageJsonPath, storageQueueMaxSize, queuePolicy, queueFullTimeoutMs, retryPolicy,
                postgresConfig, cacheConfig, moduleToggles, issues);
    }
}
