package Arcadia.ClexaGod.arcadia.config;

import Arcadia.ClexaGod.arcadia.i18n.LangKeys;
import Arcadia.ClexaGod.arcadia.storage.PostgresConfig;
import Arcadia.ClexaGod.arcadia.storage.StorageType;
import Arcadia.ClexaGod.arcadia.storage.cache.CacheConfig;
import Arcadia.ClexaGod.arcadia.logging.LogCategory;
import Arcadia.ClexaGod.arcadia.logging.LogConfig;
import Arcadia.ClexaGod.arcadia.logging.LogLevel;
import Arcadia.ClexaGod.arcadia.storage.queue.QueueFullPolicy;
import Arcadia.ClexaGod.arcadia.storage.json.JsonShardConfig;
import Arcadia.ClexaGod.arcadia.storage.json.JsonShardStrategy;
import Arcadia.ClexaGod.arcadia.storage.cache.CachePolicy;
import Arcadia.ClexaGod.arcadia.storage.maintenance.StorageHealthConfig;
import Arcadia.ClexaGod.arcadia.storage.maintenance.StorageSeedConfig;
import Arcadia.ClexaGod.arcadia.storage.maintenance.StorageMigrationConfig;
import Arcadia.ClexaGod.arcadia.storage.maintenance.StorageMigrationDirection;
import Arcadia.ClexaGod.arcadia.storage.pool.PostgresPoolAutoConfig;
import Arcadia.ClexaGod.arcadia.storage.pool.PostgresPoolAutoSizer;
import Arcadia.ClexaGod.arcadia.storage.pool.PostgresPoolSizing;
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
    private final JsonShardConfig storageJsonShardConfig;
    private final int storageQueueMaxSize;
    private final QueueFullPolicy storageQueueFullPolicy;
    private final int storageQueueFullTimeoutMs;
    private final RetryPolicy storageRetryPolicy;
    private final StorageHealthConfig storageHealthConfig;
    private final StorageSeedConfig storageSeedConfig;
    private final StorageMigrationConfig storageMigrationConfig;
    private final PostgresPoolAutoConfig postgresPoolAutoConfig;
    private final PostgresPoolSizing postgresPoolSizing;
    private final PostgresConfig postgresConfig;
    private final CacheConfig cacheConfig;
    private final LogConfig logConfig;
    private final Map<String, Boolean> moduleToggles;
    private final List<ConfigIssue> issues;

    private CoreConfig(String owner, String serverName, boolean debug, String defaultLang,
                       StorageType storageType, String storageJsonPath, JsonShardConfig storageJsonShardConfig,
                       int storageQueueMaxSize, QueueFullPolicy storageQueueFullPolicy,
                       int storageQueueFullTimeoutMs, RetryPolicy storageRetryPolicy,
                       StorageHealthConfig storageHealthConfig, StorageSeedConfig storageSeedConfig,
                       StorageMigrationConfig storageMigrationConfig, PostgresPoolAutoConfig postgresPoolAutoConfig,
                       PostgresPoolSizing postgresPoolSizing, PostgresConfig postgresConfig,
                       CacheConfig cacheConfig, LogConfig logConfig, Map<String, Boolean> moduleToggles, List<ConfigIssue> issues) {
        this.owner = owner;
        this.serverName = serverName;
        this.debug = debug;
        this.defaultLang = defaultLang;
        this.storageType = storageType;
        this.storageJsonPath = storageJsonPath;
        this.storageJsonShardConfig = storageJsonShardConfig;
        this.storageQueueMaxSize = storageQueueMaxSize;
        this.storageQueueFullPolicy = storageQueueFullPolicy;
        this.storageQueueFullTimeoutMs = storageQueueFullTimeoutMs;
        this.storageRetryPolicy = storageRetryPolicy;
        this.storageHealthConfig = storageHealthConfig;
        this.storageSeedConfig = storageSeedConfig;
        this.storageMigrationConfig = storageMigrationConfig;
        this.postgresPoolAutoConfig = postgresPoolAutoConfig;
        this.postgresPoolSizing = postgresPoolSizing;
        this.postgresConfig = postgresConfig;
        this.cacheConfig = cacheConfig;
        this.logConfig = logConfig;
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

        boolean shardEnabled = config.getBoolean("storage.json.shard.enabled", false);
        String shardStrategyRaw = config.getString("storage.json.shard.strategy", "hash").trim();
        JsonShardStrategy shardStrategy = JsonShardStrategy.from(shardStrategyRaw);
        if (shardStrategy == null) {
            issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_JSON_SHARD_STRATEGY_INVALID, shardStrategyRaw));
            shardStrategy = JsonShardStrategy.HASH;
        }
        int shardDepth = config.getInt("storage.json.shard.depth", 2);
        if (shardDepth < 1 || shardDepth > 4) {
            issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_JSON_SHARD_DEPTH_INVALID, String.valueOf(shardDepth)));
            shardDepth = 2;
        }
        int shardChars = config.getInt("storage.json.shard.chars-per-level", 2);
        if (shardChars < 1 || shardChars > 4) {
            issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_JSON_SHARD_CHARS_INVALID, String.valueOf(shardChars)));
            shardChars = 2;
        }
        boolean shardMigrate = config.getBoolean("storage.json.shard.migrate-legacy-on-read", false);
        JsonShardConfig storageJsonShardConfig = new JsonShardConfig(
                shardEnabled,
                shardStrategy,
                shardDepth,
                shardChars,
                shardMigrate
        );

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

        boolean healthEnabled = config.getBoolean("storage.health.enabled", true);
        boolean healthLogOnStartup = config.getBoolean("storage.health.log-on-startup", true);
        boolean healthJsonWriteTest = config.getBoolean("storage.health.json-write-test", true);
        boolean healthPostgresTest = config.getBoolean("storage.health.postgres-test", true);
        int healthTimeoutMs = config.getInt("storage.health.connection-timeout-ms", 3000);
        if (healthTimeoutMs <= 0) {
            issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_HEALTH_TIMEOUT_INVALID, String.valueOf(healthTimeoutMs)));
            healthTimeoutMs = 3000;
        }
        StorageHealthConfig healthConfig = new StorageHealthConfig(
                healthEnabled,
                healthLogOnStartup,
                healthJsonWriteTest,
                healthPostgresTest,
                healthTimeoutMs
        );

        boolean seedEnabled = config.getBoolean("storage.seed.enabled", true);
        boolean seedOwner = config.getBoolean("storage.seed.meta.owner", true);
        boolean seedServerName = config.getBoolean("storage.seed.meta.server-name", true);
        boolean seedCreatedAt = config.getBoolean("storage.seed.meta.created-at", true);
        StorageSeedConfig seedConfig = new StorageSeedConfig(seedEnabled, seedOwner, seedServerName, seedCreatedAt);

        boolean migrationEnabled = config.getBoolean("storage.migration.enabled", false);
        String migrationDirectionRaw = config.getString("storage.migration.direction", "json-to-postgres").trim();
        StorageMigrationDirection migrationDirection = StorageMigrationDirection.from(migrationDirectionRaw);
        if (migrationDirection == null) {
            issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_MIGRATION_DIRECTION_INVALID, migrationDirectionRaw));
            migrationDirection = StorageMigrationDirection.JSON_TO_POSTGRES;
        }
        boolean migrationDryRun = config.getBoolean("storage.migration.dry-run", true);
        int migrationMaxRecords = config.getInt("storage.migration.max-records", 10000);
        if (migrationMaxRecords <= 0) {
            issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_MIGRATION_MAX_INVALID, String.valueOf(migrationMaxRecords)));
            migrationMaxRecords = 10000;
        }
        boolean migrationSkipExisting = config.getBoolean("storage.migration.skip-existing", true);
        StorageMigrationConfig migrationConfig = new StorageMigrationConfig(
                migrationEnabled,
                migrationDirection,
                migrationDryRun,
                migrationMaxRecords,
                migrationSkipExisting
        );

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
        boolean pgPoolAutoEnabled = config.getBoolean("storage.postgresql.pool.auto.enabled", false);
        int pgPoolAutoMinSize = config.getInt("storage.postgresql.pool.auto.min-size", 8);
        int pgPoolAutoMaxSize = config.getInt("storage.postgresql.pool.auto.max-size", 32);
        int pgPoolAutoMinIdlePercent = config.getInt("storage.postgresql.pool.auto.min-idle-percent", 25);
        int pgPoolAutoCoresMultiplier = config.getInt("storage.postgresql.pool.auto.cores-multiplier", 2);
        int pgPoolAutoReserveCores = config.getInt("storage.postgresql.pool.auto.reserve-cores", 1);
        int pgPoolAutoExpectedPlayers = config.getInt("storage.postgresql.pool.auto.expected-players", 0);
        int pgPoolAutoPlayersPerConn = config.getInt("storage.postgresql.pool.auto.players-per-connection", 50);

        if (pgPoolAutoEnabled) {
            if (pgPoolAutoMinSize <= 0) {
                issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_POSTGRES_POOL_INVALID, "auto.min-size", String.valueOf(pgPoolAutoMinSize)));
                pgPoolAutoMinSize = 8;
            }
            if (pgPoolAutoMaxSize <= 0) {
                issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_POSTGRES_POOL_INVALID, "auto.max-size", String.valueOf(pgPoolAutoMaxSize)));
                pgPoolAutoMaxSize = 32;
            }
            if (pgPoolAutoMaxSize < pgPoolAutoMinSize) {
                issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_POSTGRES_POOL_INVALID, "auto.max-size", String.valueOf(pgPoolAutoMaxSize)));
                pgPoolAutoMaxSize = pgPoolAutoMinSize;
            }
            if (pgPoolAutoMinIdlePercent < 0 || pgPoolAutoMinIdlePercent > 100) {
                issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_POSTGRES_POOL_INVALID, "auto.min-idle-percent", String.valueOf(pgPoolAutoMinIdlePercent)));
                pgPoolAutoMinIdlePercent = 25;
            }
            if (pgPoolAutoCoresMultiplier <= 0) {
                issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_POSTGRES_POOL_INVALID, "auto.cores-multiplier", String.valueOf(pgPoolAutoCoresMultiplier)));
                pgPoolAutoCoresMultiplier = 2;
            }
            if (pgPoolAutoReserveCores < 0) {
                issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_POSTGRES_POOL_INVALID, "auto.reserve-cores", String.valueOf(pgPoolAutoReserveCores)));
                pgPoolAutoReserveCores = 1;
            }
            if (pgPoolAutoExpectedPlayers < 0) {
                issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_POSTGRES_POOL_INVALID, "auto.expected-players", String.valueOf(pgPoolAutoExpectedPlayers)));
                pgPoolAutoExpectedPlayers = 0;
            }
            if (pgPoolAutoPlayersPerConn <= 0) {
                issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_POSTGRES_POOL_INVALID, "auto.players-per-connection", String.valueOf(pgPoolAutoPlayersPerConn)));
                pgPoolAutoPlayersPerConn = 50;
            }
        }

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

        PostgresPoolAutoConfig poolAutoConfig = new PostgresPoolAutoConfig(
                pgPoolAutoEnabled,
                pgPoolAutoMinSize,
                pgPoolAutoMaxSize,
                pgPoolAutoMinIdlePercent,
                pgPoolAutoCoresMultiplier,
                pgPoolAutoReserveCores,
                pgPoolAutoExpectedPlayers,
                pgPoolAutoPlayersPerConn
        );
        PostgresPoolSizing poolSizing = PostgresPoolAutoSizer.calculate(poolAutoConfig);
        if (pgPoolAutoEnabled) {
            pgMaxPool = poolSizing.getMaxPoolSize();
            pgMinIdle = poolSizing.getMinIdle();
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

        boolean defaultPolicyEnabled = config.getBoolean("cache.policies.default.enabled", true);
        boolean defaultFlushOnSave = config.getBoolean("cache.policies.default.flush-on-save", false);
        int defaultFlushTimeoutMs = config.getInt("cache.policies.default.flush-timeout-ms", 2000);
        if (defaultFlushTimeoutMs <= 0) {
            issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_CACHE_POLICY_TIMEOUT_INVALID, "default", String.valueOf(defaultFlushTimeoutMs)));
            defaultFlushTimeoutMs = 2000;
        }
        CachePolicy defaultPolicy = new CachePolicy(defaultPolicyEnabled, defaultFlushOnSave, java.time.Duration.ofMillis(defaultFlushTimeoutMs));

        Map<String, CachePolicy> policyOverrides = new LinkedHashMap<>();
        ConfigSection policySection = config.getSection("cache.policies.repos");
        for (String repoName : policySection.keySet()) {
            if (repoName == null || repoName.isBlank()) {
                continue;
            }
            String basePath = "cache.policies.repos." + repoName + ".";
            boolean enabled = config.getBoolean(basePath + "enabled", defaultPolicyEnabled);
            boolean flushOnSave = config.getBoolean(basePath + "flush-on-save", defaultFlushOnSave);
            int timeoutMs = config.getInt(basePath + "flush-timeout-ms", defaultFlushTimeoutMs);
            if (timeoutMs <= 0) {
                issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_CACHE_POLICY_TIMEOUT_INVALID, repoName, String.valueOf(timeoutMs)));
                timeoutMs = defaultFlushTimeoutMs;
            }
            CachePolicy policy = new CachePolicy(enabled, flushOnSave, java.time.Duration.ofMillis(timeoutMs));
            policyOverrides.put(repoName.toLowerCase(Locale.ROOT), policy);
        }

        CacheConfig cacheConfig = new CacheConfig(
                cacheEnabled,
                cacheTtlSeconds,
                cacheMaxEntries,
                cacheFlushInterval,
                warmupEnabled,
                warmupMaxEntries,
                warmupDelaySeconds,
                flushOnPlayerQuit,
                defaultPolicy,
                policyOverrides
        );

        String logDefaultRaw = config.getString("logging.default-level", "info").trim();
        LogLevel defaultLevel = LogLevel.from(logDefaultRaw);
        if (defaultLevel == null) {
            issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_LOG_LEVEL_INVALID, "default", logDefaultRaw));
            defaultLevel = LogLevel.INFO;
        }
        boolean useCoreDebug = config.getBoolean("logging.use-core-debug", true);
        boolean includeCategoryPrefix = config.getBoolean("logging.include-category-prefix", true);
        Map<LogCategory, LogLevel> categoryLevels = new LinkedHashMap<>();
        ConfigSection logCategorySection = config.getSection("logging.categories");
        for (Map.Entry<String, Object> entry : logCategorySection.entrySet()) {
            String key = entry.getKey();
            LogCategory category = LogCategory.from(key);
            if (category == null) {
                issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_LOG_CATEGORY_INVALID, key));
                continue;
            }
            String levelRaw = String.valueOf(entry.getValue());
            LogLevel level = LogLevel.from(levelRaw);
            if (level == null) {
                issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_LOG_LEVEL_INVALID, category.getId(), levelRaw));
                level = defaultLevel;
            }
            categoryLevels.put(category, level);
        }
        LogConfig logConfig = new LogConfig(defaultLevel, useCoreDebug, includeCategoryPrefix, categoryLevels);

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
                storageType, storageJsonPath, storageJsonShardConfig,
                storageQueueMaxSize, queuePolicy, queueFullTimeoutMs, retryPolicy,
                healthConfig, seedConfig, migrationConfig, poolAutoConfig, poolSizing,
                postgresConfig, cacheConfig, logConfig, moduleToggles, issues);
    }
}
