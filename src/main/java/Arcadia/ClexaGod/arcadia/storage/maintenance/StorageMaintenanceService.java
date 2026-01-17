package Arcadia.ClexaGod.arcadia.storage.maintenance;

import Arcadia.ClexaGod.arcadia.config.CoreConfig;
import Arcadia.ClexaGod.arcadia.i18n.LangKeys;
import Arcadia.ClexaGod.arcadia.logging.LogCategory;
import Arcadia.ClexaGod.arcadia.logging.LogService;
import Arcadia.ClexaGod.arcadia.storage.PostgresConfig;
import Arcadia.ClexaGod.arcadia.storage.PostgresStorageProvider;
import Arcadia.ClexaGod.arcadia.storage.StorageManager;
import Arcadia.ClexaGod.arcadia.storage.StorageProvider;
import Arcadia.ClexaGod.arcadia.storage.repository.StorageRepository;
import Arcadia.ClexaGod.arcadia.storage.repository.json.JsonRepository;
import Arcadia.ClexaGod.arcadia.storage.repository.meta.MetaJsonCodec;
import Arcadia.ClexaGod.arcadia.storage.repository.meta.MetaRecord;
import Arcadia.ClexaGod.arcadia.storage.repository.meta.PostgresMetaRepository;
import Arcadia.ClexaGod.arcadia.storage.migration.MigrationManager;
import Arcadia.ClexaGod.arcadia.storage.migration.PostgresMigrations;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.allaymc.api.message.I18n;
import org.allaymc.api.scheduler.TaskCreator;
import org.allaymc.api.server.Server;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class StorageMaintenanceService {

    private static final String META_OWNER = "owner";
    private static final String META_SERVER_NAME = "server-name";
    private static final String META_CREATED_AT = "created-at";

    private final CoreConfig config;
    private final StorageManager storageManager;
    private final StorageRepository<MetaRecord> metaRepository;
    private final Path dataFolder;
    private final LogService logService;

    public StorageMaintenanceService(CoreConfig config,
                                     StorageManager storageManager,
                                     StorageRepository<MetaRecord> metaRepository,
                                     Path dataFolder,
                                     LogService logService) {
        this.config = Objects.requireNonNull(config, "config");
        this.storageManager = Objects.requireNonNull(storageManager, "storageManager");
        this.metaRepository = metaRepository;
        this.dataFolder = Objects.requireNonNull(dataFolder, "dataFolder");
        this.logService = Objects.requireNonNull(logService, "logService");
    }

    public void runAsync(TaskCreator taskCreator) {
        Server.getInstance().getScheduler().runLaterAsync(taskCreator, this::run);
    }

    public void run() {
        runHealthCheck(config.getStorageHealthConfig());
        runDataMigration(config.getStorageMigrationConfig());
        runSeed(config.getStorageSeedConfig());
    }

    private void runHealthCheck(StorageHealthConfig healthConfig) {
        if (healthConfig == null || !healthConfig.isEnabled() || !healthConfig.isLogOnStartup()) {
            return;
        }

        if (healthConfig.isJsonWriteTest()) {
            Path jsonRoot = resolveJsonRoot();
            try {
                Files.createDirectories(jsonRoot);
                Path testPath = jsonRoot.resolve(".arcadia-health-" + System.currentTimeMillis() + ".tmp");
                Files.writeString(testPath, "ok", StandardCharsets.UTF_8);
                Files.deleteIfExists(testPath);
                logService.info(LogCategory.MAINTENANCE,
                        I18n.get().tr(LangKeys.LOG_STORAGE_HEALTH_JSON_OK, jsonRoot.toString()));
            } catch (Exception e) {
                logService.error(LogCategory.MAINTENANCE,
                        I18n.get().tr(LangKeys.LOG_STORAGE_HEALTH_JSON_FAIL, jsonRoot.toString()), e);
            }
        }

        if (healthConfig.isPostgresTest()) {
            PostgresConfig pg = config.getPostgresConfig();
            if (pg == null || !pg.isValid()) {
                logService.error(LogCategory.MAINTENANCE,
                        I18n.get().tr(LangKeys.LOG_STORAGE_HEALTH_POSTGRES_FAIL, "invalid-config"));
                return;
            }
            DataSourceHolder holder = resolvePostgresDataSource(
                    pg,
                    healthConfig.getConnectionTimeoutMs(),
                    false,
                    LangKeys.LOG_STORAGE_HEALTH_POSTGRES_FAIL
            );
            if (holder == null) {
                return;
            }
            try (Connection connection = holder.dataSource().getConnection();
                 PreparedStatement ps = connection.prepareStatement("SELECT 1");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    logService.info(LogCategory.MAINTENANCE,
                            I18n.get().tr(LangKeys.LOG_STORAGE_HEALTH_POSTGRES_OK, pg.getHost(), pg.getDatabase()));
                } else {
                    logService.error(LogCategory.MAINTENANCE,
                            I18n.get().tr(LangKeys.LOG_STORAGE_HEALTH_POSTGRES_FAIL, "no-result"));
                }
            } catch (Exception e) {
                logService.error(LogCategory.MAINTENANCE,
                        I18n.get().tr(LangKeys.LOG_STORAGE_HEALTH_POSTGRES_FAIL, e.getMessage()), e);
            } finally {
                holder.closeIfNeeded();
            }
        }
    }

    private void runSeed(StorageSeedConfig seedConfig) {
        if (seedConfig == null || !seedConfig.isEnabled()) {
            return;
        }
        if (metaRepository == null) {
            logService.warn(LogCategory.MAINTENANCE,
                    I18n.get().tr(LangKeys.LOG_STORAGE_SEED_SKIPPED, "meta-repository-missing"));
            return;
        }

        logService.info(LogCategory.MAINTENANCE, I18n.get().tr(LangKeys.LOG_STORAGE_SEED_START));
        int seeded = 0;
        int skipped = 0;

        if (seedConfig.isSeedOwner()) {
            if (metaRepository.exists(META_OWNER)) {
                skipped++;
            } else {
                metaRepository.save(new MetaRecord(META_OWNER, config.getOwner()));
                seeded++;
            }
        }
        if (seedConfig.isSeedServerName()) {
            if (metaRepository.exists(META_SERVER_NAME)) {
                skipped++;
            } else {
                metaRepository.save(new MetaRecord(META_SERVER_NAME, config.getServerName()));
                seeded++;
            }
        }
        if (seedConfig.isSeedCreatedAt()) {
            if (metaRepository.exists(META_CREATED_AT)) {
                skipped++;
            } else {
                metaRepository.save(new MetaRecord(META_CREATED_AT, Instant.now().toString()));
                seeded++;
            }
        }

        logService.info(LogCategory.MAINTENANCE,
                I18n.get().tr(LangKeys.LOG_STORAGE_SEED_COMPLETE, seeded, skipped));
    }

    private void runDataMigration(StorageMigrationConfig migrationConfig) {
        if (migrationConfig == null || !migrationConfig.isEnabled()) {
            return;
        }

        StorageMigrationDirection direction = migrationConfig.getDirection();
        if (direction == null) {
            logService.warn(LogCategory.MAINTENANCE,
                    I18n.get().tr(LangKeys.LOG_STORAGE_DATA_MIGRATION_SKIPPED, "direction-missing"));
            return;
        }
        PostgresConfig pgConfig = config.getPostgresConfig();
        if (pgConfig == null || !pgConfig.isValid()) {
            logService.warn(LogCategory.MAINTENANCE,
                    I18n.get().tr(LangKeys.LOG_STORAGE_DATA_MIGRATION_SKIPPED, "postgres-config-invalid"));
            return;
        }

        StorageRepository<MetaRecord> source = null;
        StorageRepository<MetaRecord> target = null;
        DataSourceHolder postgresHolder = null;
        try {
            Path jsonRoot = resolveJsonRoot().resolve("meta");
            if (direction == StorageMigrationDirection.JSON_TO_POSTGRES) {
                source = new JsonRepository<>(
                        "meta",
                        jsonRoot,
                        new MetaJsonCodec(),
                        logService,
                        config.getStorageRetryPolicy(),
                        config.getStorageJsonShardConfig()
                );
                postgresHolder = resolvePostgresDataSource(
                        pgConfig,
                        pgConfig.getConnectionTimeoutMs(),
                        true,
                        LangKeys.LOG_STORAGE_DATA_MIGRATION_FAILED
                );
                if (postgresHolder != null) {
                    target = new PostgresMetaRepository(postgresHolder.dataSource(), logService, config.getStorageRetryPolicy());
                }
            } else if (direction == StorageMigrationDirection.POSTGRES_TO_JSON) {
                postgresHolder = resolvePostgresDataSource(
                        pgConfig,
                        pgConfig.getConnectionTimeoutMs(),
                        true,
                        LangKeys.LOG_STORAGE_DATA_MIGRATION_FAILED
                );
                if (postgresHolder != null) {
                    source = new PostgresMetaRepository(postgresHolder.dataSource(), logService, config.getStorageRetryPolicy());
                }
                Files.createDirectories(jsonRoot);
                target = new JsonRepository<>(
                        "meta",
                        jsonRoot,
                        new MetaJsonCodec(),
                        logService,
                        config.getStorageRetryPolicy(),
                        config.getStorageJsonShardConfig()
                );
            }

            if (source == null || target == null) {
                logService.warn(LogCategory.MAINTENANCE,
                        I18n.get().tr(LangKeys.LOG_STORAGE_DATA_MIGRATION_SKIPPED, "repository-unavailable"));
                return;
            }

            int limit = migrationConfig.getMaxRecords();
            List<MetaRecord> records = source.loadAll(limit);
            if (records.isEmpty()) {
                logService.info(LogCategory.MAINTENANCE,
                        I18n.get().tr(LangKeys.LOG_STORAGE_DATA_MIGRATION_SKIPPED, "no-records"));
                return;
            }

            logService.info(LogCategory.MAINTENANCE, I18n.get().tr(
                    LangKeys.LOG_STORAGE_DATA_MIGRATION_START,
                    direction.getId(),
                    limit,
                    migrationConfig.isDryRun(),
                    migrationConfig.isSkipExisting()
            ));

            int migrated = 0;
            int skipped = 0;
            int failed = 0;

            for (MetaRecord record : records) {
                if (migrationConfig.isSkipExisting() && target.exists(record.getId())) {
                    skipped++;
                    continue;
                }
                if (!migrationConfig.isDryRun()) {
                    try {
                        target.save(record);
                    } catch (Exception e) {
                        failed++;
                        logService.error(LogCategory.MAINTENANCE,
                                I18n.get().tr(LangKeys.LOG_STORAGE_DATA_MIGRATION_FAILED, record.getId()), e);
                        continue;
                    }
                }
                migrated++;
            }

            logService.info(LogCategory.MAINTENANCE, I18n.get().tr(
                    LangKeys.LOG_STORAGE_DATA_MIGRATION_COMPLETE,
                    migrated,
                    skipped,
                    failed
            ));
        } catch (Exception e) {
            logService.error(LogCategory.MAINTENANCE,
                    I18n.get().tr(LangKeys.LOG_STORAGE_DATA_MIGRATION_FAILED, "fatal"), e);
        } finally {
            if (postgresHolder != null) {
                postgresHolder.closeIfNeeded();
            }
        }
    }

    private Path resolveJsonRoot() {
        Path path = Path.of(config.getStorageJsonPath());
        if (path.isAbsolute()) {
            return path;
        }
        return dataFolder.resolve(path);
    }

    private DataSourceHolder resolvePostgresDataSource(PostgresConfig pgConfig,
                                                       int timeoutMs,
                                                       boolean runMigrations,
                                                       String errorKey) {
        if (pgConfig == null || !pgConfig.isValid()) {
            return null;
        }
        StorageProvider provider = storageManager.getProvider();
        if (provider instanceof PostgresStorageProvider pgProvider && pgProvider.getDataSource() != null) {
            return new DataSourceHolder(pgProvider.getDataSource(), false);
        }

        try {
            Class.forName("org.postgresql.Driver");
            HikariConfig hikari = new HikariConfig();
            hikari.setJdbcUrl(pgConfig.buildJdbcUrl());
            hikari.setDriverClassName("org.postgresql.Driver");
            hikari.setUsername(pgConfig.getUsername());
            hikari.setPassword(pgConfig.getPassword());
            hikari.setMaximumPoolSize(1);
            hikari.setMinimumIdle(0);
            hikari.setConnectionTimeout(Math.max(1000, timeoutMs));
            hikari.setIdleTimeout(Math.min(pgConfig.getIdleTimeoutMs(), 60000));
            hikari.setPoolName("ArcadiaMaintenance");
            HikariDataSource dataSource = new HikariDataSource(hikari);
            if (runMigrations) {
                new MigrationManager(dataSource, logService).migrate(PostgresMigrations.list());
            }
            return new DataSourceHolder(dataSource, true);
        } catch (Exception e) {
            if (errorKey != null) {
                logService.error(LogCategory.MAINTENANCE, I18n.get().tr(errorKey, "init-failed"), e);
            }
            return null;
        }
    }

    private record DataSourceHolder(DataSource dataSource, boolean closeOnExit) {

        void closeIfNeeded() {
            if (closeOnExit && dataSource instanceof HikariDataSource hikari) {
                hikari.close();
            }
        }
    }
}
