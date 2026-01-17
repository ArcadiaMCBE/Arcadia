package Arcadia.ClexaGod.arcadia.health;

import Arcadia.ClexaGod.arcadia.ArcadiaCore;
import Arcadia.ClexaGod.arcadia.config.CoreConfig;
import Arcadia.ClexaGod.arcadia.i18n.I18nUtil;
import Arcadia.ClexaGod.arcadia.i18n.LangKeys;
import Arcadia.ClexaGod.arcadia.logging.LogConfig;
import Arcadia.ClexaGod.arcadia.storage.JsonStorageProvider;
import Arcadia.ClexaGod.arcadia.storage.PostgresConfig;
import Arcadia.ClexaGod.arcadia.storage.PostgresStorageProvider;
import Arcadia.ClexaGod.arcadia.storage.StorageManager;
import Arcadia.ClexaGod.arcadia.storage.StorageProvider;
import Arcadia.ClexaGod.arcadia.storage.StorageType;
import Arcadia.ClexaGod.arcadia.storage.cache.CacheConfig;
import Arcadia.ClexaGod.arcadia.storage.cache.CacheMetrics;
import Arcadia.ClexaGod.arcadia.storage.cache.StorageCacheManager;
import Arcadia.ClexaGod.arcadia.storage.queue.AsyncWriteQueue;
import Arcadia.ClexaGod.arcadia.storage.retry.RetryPolicy;
import Arcadia.ClexaGod.arcadia.util.TimeUtil;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.allaymc.api.command.CommandSender;
import org.allaymc.api.server.Server;
import org.allaymc.api.utils.TextFormat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class HealthReportService {

    private final ArcadiaCore core;

    public HealthReportService(ArcadiaCore core) {
        this.core = Objects.requireNonNull(core, "core");
    }

    public List<String> buildReport(CommandSender sender, boolean includePerRepo, boolean runChecks) {
        List<String> lines = new ArrayList<>();
        CoreConfig config = core.getConfigService().getCoreConfig();
        StorageManager storageManager = core.getStorageManager();
        StorageProvider provider = storageManager != null ? storageManager.getProvider() : null;
        StorageType requestedType = config.getStorageType();
        String version = core.getPluginContainer().descriptor().getVersion();
        String apiVersion = core.getPluginContainer().descriptor().getAPIVersion();

        lines.add(TextFormat.AQUA + I18nUtil.tr(sender, LangKeys.COMMAND_ARCADIA_HEALTH_HEADER));
        lines.add(TextFormat.DARK_GRAY + repeat("-", 42));

        lines.add(section(sender, LangKeys.COMMAND_ARCADIA_HEALTH_SECTION_CORE));
        lines.add(TextFormat.GRAY + I18nUtil.tr(
                sender,
                LangKeys.COMMAND_ARCADIA_HEALTH_LINE_CORE,
                value(version),
                value(apiVersion),
                value(TimeUtil.formatDuration(System.currentTimeMillis() - Server.getInstance().getStartTime())),
                value(String.valueOf(Server.getInstance().getTick()))
        ));
        lines.add(TextFormat.GRAY + I18nUtil.tr(
                sender,
                LangKeys.COMMAND_ARCADIA_HEALTH_LINE_PLAYERS,
                value(String.valueOf(Server.getInstance().getPlayerManager().getPlayerCount())),
                value(String.valueOf(Server.getInstance().getPlayerManager().getMaxPlayerCount())),
                value(String.valueOf(Server.getInstance().getWorldPool().getWorlds().size())),
                value(Server.getInstance().getWorldPool().getDefaultWorld().getName())
        ));

        LogConfig logConfig = config.getLogConfig();
        int overrides = logConfig.getCategoryLevels() != null ? logConfig.getCategoryLevels().size() : 0;
        lines.add(TextFormat.GRAY + I18nUtil.tr(
                sender,
                LangKeys.COMMAND_ARCADIA_HEALTH_LINE_LOGGING,
                value(logConfig.getDefaultLevel().name().toLowerCase(Locale.ROOT)),
                value(String.valueOf(logConfig.isUseCoreDebug() && config.isDebug())),
                value(String.valueOf(logConfig.isIncludeCategoryPrefix())),
                value(String.valueOf(overrides))
        ));

        lines.add(section(sender, LangKeys.COMMAND_ARCADIA_HEALTH_SECTION_RUNTIME));
        Runtime runtime = Runtime.getRuntime();
        long total = runtime.totalMemory() / (1024 * 1024);
        long free = runtime.freeMemory() / (1024 * 1024);
        long max = runtime.maxMemory() / (1024 * 1024);
        long used = Math.max(0, total - free);
        lines.add(TextFormat.GRAY + I18nUtil.tr(
                sender,
                LangKeys.COMMAND_ARCADIA_HEALTH_LINE_RUNTIME,
                value(String.valueOf(used)),
                value(String.valueOf(free)),
                value(String.valueOf(total)),
                value(String.valueOf(max))
        ));

        lines.add(section(sender, LangKeys.COMMAND_ARCADIA_HEALTH_SECTION_STORAGE));
        if (provider == null) {
            lines.add(TextFormat.RED + I18nUtil.tr(sender, LangKeys.COMMAND_ARCADIA_HEALTH_LINE_STORAGE_NONE));
        } else if (provider instanceof JsonStorageProvider jsonProvider) {
            lines.add(TextFormat.GRAY + I18nUtil.tr(
                    sender,
                    LangKeys.COMMAND_ARCADIA_HEALTH_LINE_STORAGE_JSON,
                    value(provider.getType().getId()),
                    value(requestedType.getId()),
                    formatFlag(provider.isReady()),
                    value(jsonProvider.getRootPath().toString())
            ));
            lines.add(TextFormat.GRAY + I18nUtil.tr(
                    sender,
                    LangKeys.COMMAND_ARCADIA_HEALTH_LINE_STORAGE_JSON_SHARD,
                    formatFlag(config.getStorageJsonShardConfig().isEnabled()),
                    value(config.getStorageJsonShardConfig().getStrategy().getId()),
                    value(String.valueOf(config.getStorageJsonShardConfig().getDepth())),
                    value(String.valueOf(config.getStorageJsonShardConfig().getCharsPerLevel())),
                    formatFlag(config.getStorageJsonShardConfig().isMigrateLegacyOnRead())
            ));
        } else if (provider instanceof PostgresStorageProvider pgProvider) {
            PoolInfo poolInfo = readPoolInfo(pgProvider);
            lines.add(TextFormat.GRAY + I18nUtil.tr(
                    sender,
                    LangKeys.COMMAND_ARCADIA_HEALTH_LINE_STORAGE_PG,
                    value(provider.getType().getId()),
                    value(requestedType.getId()),
                    formatFlag(provider.isReady()),
                    value(pgProvider.getConfig().getHost()),
                    value(pgProvider.getConfig().getDatabase()),
                    value(poolInfo.active),
                    value(poolInfo.total),
                    value(poolInfo.idle)
            ));
        } else {
            lines.add(TextFormat.GRAY + I18nUtil.tr(
                    sender,
                    LangKeys.COMMAND_ARCADIA_HEALTH_LINE_STORAGE_JSON,
                    value(provider.getType().getId()),
                    value(requestedType.getId()),
                    formatFlag(provider.isReady()),
                    value(config.getStorageJsonPath())
            ));
        }

        RetryPolicy retryPolicy = config.getStorageRetryPolicy();
        lines.add(TextFormat.GRAY + I18nUtil.tr(
                sender,
                LangKeys.COMMAND_ARCADIA_HEALTH_LINE_RETRY,
                formatFlag(retryPolicy.enabled()),
                value(String.valueOf(retryPolicy.maxAttempts())),
                value(String.valueOf(retryPolicy.baseDelayMs())),
                value(String.valueOf(retryPolicy.maxDelayMs())),
                value(String.valueOf(retryPolicy.jitterMs()))
        ));

        lines.add(section(sender, LangKeys.COMMAND_ARCADIA_HEALTH_SECTION_QUEUE));
        AsyncWriteQueue queue = storageManager != null ? storageManager.getWriteQueue() : null;
        if (queue == null) {
            lines.add(TextFormat.RED + I18nUtil.tr(sender, LangKeys.COMMAND_ARCADIA_HEALTH_LINE_QUEUE_NONE));
        } else {
            lines.add(TextFormat.GRAY + I18nUtil.tr(
                    sender,
                    LangKeys.COMMAND_ARCADIA_HEALTH_LINE_QUEUE,
                    value(String.valueOf(queue.getQueueSize())),
                    value(String.valueOf(queue.getMaxQueueSize())),
                    value(queue.getFullPolicy().name().toLowerCase(Locale.ROOT)),
                    value(String.valueOf(queue.getFullTimeoutMs())),
                    formatFlag(queue.isStarted())
            ));
        }

        lines.add(section(sender, LangKeys.COMMAND_ARCADIA_HEALTH_SECTION_CACHE));
        CacheConfig cacheConfig = config.getCacheConfig();
        lines.add(TextFormat.GRAY + I18nUtil.tr(
                sender,
                LangKeys.COMMAND_ARCADIA_HEALTH_LINE_CACHE,
                formatFlag(cacheConfig.isEnabled()),
                value(String.valueOf(cacheConfig.getTtlSeconds())),
                value(String.valueOf(cacheConfig.getMaxEntries())),
                value(String.valueOf(cacheConfig.getFlushIntervalSeconds())),
                formatFlag(cacheConfig.isWarmupEnabled())
        ));
        StorageCacheManager cacheManager = storageManager != null ? storageManager.getCacheManager() : null;
        if (cacheManager == null) {
            lines.add(TextFormat.RED + I18nUtil.tr(sender, LangKeys.COMMAND_ARCADIA_HEALTH_LINE_CACHE_NONE));
        } else {
            CacheMetrics.CacheMetricsSnapshot snapshot = cacheManager.snapshotMetrics();
            lines.add(TextFormat.GRAY + I18nUtil.tr(
                    sender,
                    LangKeys.COMMAND_ARCADIA_HEALTH_LINE_CACHE_METRICS,
                    value(String.valueOf(snapshot.hits())),
                    value(String.valueOf(snapshot.misses())),
                    value(formatPercent(snapshot.hitRate())),
                    value(String.valueOf(snapshot.flushes())),
                    value(String.valueOf(snapshot.writeTasks())),
                    value(String.valueOf(snapshot.evictedExpired())),
                    value(String.valueOf(snapshot.evictedOverflow())),
                    value(String.valueOf(snapshot.lastQueueSize())),
                    value(String.valueOf(snapshot.maxQueueSize()))
            ));
            if (includePerRepo) {
                for (CacheMetrics.CacheMetricsSnapshot perRepo : cacheManager.snapshotMetricsPerRepo()) {
                    lines.add(TextFormat.DARK_GRAY + I18nUtil.tr(
                            sender,
                            LangKeys.COMMAND_ARCADIA_HEALTH_LINE_CACHE_REPO,
                            value(perRepo.name()),
                            value(String.valueOf(perRepo.hits())),
                            value(String.valueOf(perRepo.misses())),
                            value(formatPercent(perRepo.hitRate())),
                            value(String.valueOf(perRepo.flushes())),
                            value(String.valueOf(perRepo.writeTasks())),
                            value(String.valueOf(perRepo.evictedExpired())),
                            value(String.valueOf(perRepo.evictedOverflow()))
                    ));
                }
            }
        }

        if (provider instanceof PostgresStorageProvider pgProvider) {
            lines.add(section(sender, LangKeys.COMMAND_ARCADIA_HEALTH_SECTION_DB));
            PostgresConfig pgConfig = pgProvider.getConfig();
            PoolInfo poolInfo = readPoolInfo(pgProvider);
            lines.add(TextFormat.GRAY + I18nUtil.tr(
                    sender,
                    LangKeys.COMMAND_ARCADIA_HEALTH_LINE_DB_POOL,
                    value(pgConfig.getHost()),
                    value(pgConfig.getDatabase()),
                    value(poolInfo.active),
                    value(poolInfo.total),
                    value(poolInfo.idle),
                    value(String.valueOf(pgConfig.getMaxPoolSize()))
            ));
        }

        if (runChecks) {
            lines.add(section(sender, LangKeys.COMMAND_ARCADIA_HEALTH_SECTION_CHECKS));
            CheckResult jsonCheck = checkJson(config, provider);
            lines.add(TextFormat.GRAY + I18nUtil.tr(
                    sender,
                    LangKeys.COMMAND_ARCADIA_HEALTH_LINE_CHECK_JSON,
                    formatFlag(jsonCheck.ok),
                    value(jsonCheck.detail)
            ));
            CheckResult pgCheck = checkPostgres(config, provider);
            lines.add(TextFormat.GRAY + I18nUtil.tr(
                    sender,
                    LangKeys.COMMAND_ARCADIA_HEALTH_LINE_CHECK_PG,
                    formatFlag(pgCheck.ok),
                    value(pgCheck.detail)
            ));
        }

        return lines;
    }

    private PoolInfo readPoolInfo(PostgresStorageProvider provider) {
        HikariDataSource dataSource = provider.getDataSource();
        if (dataSource == null) {
            return new PoolInfo("0", "0", "0");
        }
        HikariPoolMXBean mxBean = dataSource.getHikariPoolMXBean();
        if (mxBean == null) {
            return new PoolInfo("0", "0", "0");
        }
        return new PoolInfo(
                String.valueOf(mxBean.getActiveConnections()),
                String.valueOf(mxBean.getTotalConnections()),
                String.valueOf(mxBean.getIdleConnections())
        );
    }

    private CheckResult checkJson(CoreConfig config, StorageProvider provider) {
        try {
            Path root;
            if (provider instanceof JsonStorageProvider jsonProvider) {
                root = jsonProvider.getRootPath();
            } else {
                Path configured = Path.of(config.getStorageJsonPath());
                root = configured.isAbsolute()
                        ? configured
                        : core.getPluginContainer().dataFolder().resolve(configured);
            }
            Files.createDirectories(root);
            Path testPath = root.resolve(".arcadia-health-check.tmp");
            Files.writeString(testPath, "ok", StandardCharsets.UTF_8);
            Files.deleteIfExists(testPath);
            return new CheckResult(true, root.toString());
        } catch (Exception e) {
            return new CheckResult(false, safeDetail(e));
        }
    }

    private CheckResult checkPostgres(CoreConfig config, StorageProvider provider) {
        PostgresConfig pgConfig = config.getPostgresConfig();
        if (pgConfig == null || !pgConfig.isValid()) {
            return new CheckResult(false, "invalid-config");
        }
        if (provider instanceof PostgresStorageProvider pgProvider && pgProvider.getDataSource() != null) {
            return runPostgresCheck(pgProvider.getDataSource());
        }
        try {
            Class.forName("org.postgresql.Driver");
            com.zaxxer.hikari.HikariConfig hikari = new com.zaxxer.hikari.HikariConfig();
            hikari.setJdbcUrl(pgConfig.buildJdbcUrl());
            hikari.setDriverClassName("org.postgresql.Driver");
            hikari.setUsername(pgConfig.getUsername());
            hikari.setPassword(pgConfig.getPassword());
            hikari.setMaximumPoolSize(1);
            hikari.setMinimumIdle(0);
            hikari.setConnectionTimeout(Math.max(1000, config.getStorageHealthConfig().getConnectionTimeoutMs()));
            hikari.setPoolName("ArcadiaHealthCheck");
            try (HikariDataSource dataSource = new HikariDataSource(hikari)) {
                return runPostgresCheck(dataSource);
            }
        } catch (Exception e) {
            return new CheckResult(false, safeDetail(e));
        }
    }

    private CheckResult runPostgresCheck(javax.sql.DataSource dataSource) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return new CheckResult(true, "ok");
            }
            return new CheckResult(false, "no-result");
        } catch (Exception e) {
            return new CheckResult(false, safeDetail(e));
        }
    }

    private String formatFlag(boolean value) {
        return (value ? TextFormat.GREEN : TextFormat.RED) + String.valueOf(value) + TextFormat.RESET;
    }

    private String formatPercent(double value) {
        double percent = value * 100.0;
        return String.format(Locale.ROOT, "%.2f", percent);
    }

    private String value(String raw) {
        return TextFormat.WHITE + raw + TextFormat.RESET;
    }

    private String section(CommandSender sender, String key) {
        return TextFormat.GOLD + "[" + I18nUtil.tr(sender, key) + "]" + TextFormat.RESET;
    }

    private String repeat(String ch, int count) {
        return ch.repeat(Math.max(0, count));
    }

    private String safeDetail(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }

    private record PoolInfo(String active, String total, String idle) {
    }

    private record CheckResult(boolean ok, String detail) {
    }
}
