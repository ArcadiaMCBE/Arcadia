package Arcadia.ClexaGod.arcadia.storage;

import Arcadia.ClexaGod.arcadia.config.CoreConfig;
import Arcadia.ClexaGod.arcadia.i18n.LangKeys;
import Arcadia.ClexaGod.arcadia.logging.LogCategory;
import Arcadia.ClexaGod.arcadia.logging.LogService;
import Arcadia.ClexaGod.arcadia.storage.cache.CachePolicy;
import Arcadia.ClexaGod.arcadia.storage.cache.StorageCacheManager;
import Arcadia.ClexaGod.arcadia.storage.queue.AsyncWriteQueue;
import Arcadia.ClexaGod.arcadia.storage.queue.QueueFullPolicy;
import Arcadia.ClexaGod.arcadia.storage.model.StorageRecord;
import Arcadia.ClexaGod.arcadia.storage.repository.StorageRepository;
import Arcadia.ClexaGod.arcadia.storage.retry.RetryPolicy;
import Arcadia.ClexaGod.arcadia.storage.pool.PostgresPoolSizing;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.allaymc.api.message.I18n;
import org.allaymc.api.scheduler.TaskCreator;
import org.allaymc.api.server.Server;

import java.nio.file.Path;
import java.time.Duration;

@RequiredArgsConstructor
public final class StorageManager {

    private final LogService logService;
    private final Path dataFolder;
    private final TaskCreator taskCreator;
    @Getter
    private AsyncWriteQueue writeQueue;
    private StorageProvider provider;
    @Getter
    private StorageCacheManager cacheManager;
    @Getter
    private RetryPolicy retryPolicy;

    public void init(CoreConfig config) {
        int maxQueueSize = config.getStorageQueueMaxSize();
        QueueFullPolicy queuePolicy = config.getStorageQueueFullPolicy();
        int queueTimeoutMs = config.getStorageQueueFullTimeoutMs();
        if (writeQueue == null
                || writeQueue.getMaxQueueSize() != maxQueueSize
                || writeQueue.getFullPolicy() != queuePolicy
                || writeQueue.getFullTimeoutMs() != queueTimeoutMs) {
            if (writeQueue != null && writeQueue.isStarted()) {
                writeQueue.shutdown(Duration.ofSeconds(10));
            }
            writeQueue = new AsyncWriteQueue(Server.getInstance().getVirtualThreadPool(), logService, maxQueueSize, queuePolicy, queueTimeoutMs);
        }
        if (!writeQueue.isStarted()) {
            writeQueue.start();
        }
        retryPolicy = config.getStorageRetryPolicy();
        StorageType requested = config.getStorageType();
        logService.info(LogCategory.STORAGE, I18n.get().tr(LangKeys.LOG_STORAGE_SELECTED, requested.getId()));
        if (requested == StorageType.POSTGRESQL) {
            PostgresPoolSizing sizing = config.getPostgresPoolSizing();
            if (sizing != null && sizing.isEnabled()) {
                logService.info(LogCategory.STORAGE, I18n.get().tr(
                        LangKeys.LOG_STORAGE_POSTGRES_POOL_AUTO_APPLIED,
                        sizing.getAvailableCores(),
                        sizing.getUsableCores(),
                        sizing.getTargetFromCores(),
                        sizing.getTargetFromPlayers(),
                        sizing.getMaxPoolSize(),
                        sizing.getMinIdle()
                ));
            }
        }

        provider = createProvider(requested, config);
        if (provider == null) {
            fallbackToJson(requested, config);
        } else {
            try {
                provider.init();
                logService.info(LogCategory.STORAGE, I18n.get().tr(LangKeys.LOG_STORAGE_INIT_SUCCESS, provider.getType().getId()));
            } catch (Exception e) {
                logService.error(LogCategory.STORAGE, I18n.get().tr(LangKeys.LOG_STORAGE_INIT_FAILED, provider.getType().getId()), e);
                if (requested != StorageType.JSON) {
                    fallbackToJson(requested, config);
                }
            }
        }

        if (cacheManager == null) {
            cacheManager = new StorageCacheManager(
                    config.getCacheConfig(),
                    logService,
                    writeQueue,
                    Server.getInstance().getScheduler(),
                    taskCreator
            );
            cacheManager.start();
        }
    }

    public void close() {
        if (cacheManager != null) {
            cacheManager.shutdown();
            cacheManager = null;
        }
        if (writeQueue != null && writeQueue.isStarted()) {
            writeQueue.shutdown(Duration.ofSeconds(10));
        }
        if (provider != null) {
            provider.close();
            provider = null;
        }
        retryPolicy = null;
    }

    public StorageProvider getProvider() {
        return provider;
    }

    public <T extends StorageRecord> StorageRepository<T> withCache(StorageRepository<T> repository) {
        if (cacheManager == null) {
            return repository;
        }
        return cacheManager.wrap(repository);
    }

    public <T extends StorageRecord> StorageRepository<T> withCache(StorageRepository<T> repository, CachePolicy policy) {
        if (cacheManager == null) {
            return repository;
        }
        return cacheManager.wrap(repository, policy);
    }

    public void flushCaches() {
        if (cacheManager != null) {
            cacheManager.flushAll();
        }
    }

    public void flushCachesAsync() {
        if (cacheManager == null) {
            return;
        }
        Server.getInstance().getScheduler().runLaterAsync(taskCreator, cacheManager::flushAll);
    }

    public void scheduleWarmUp() {
        if (cacheManager != null) {
            cacheManager.warmUp();
        }
    }

    private StorageProvider createProvider(StorageType type, CoreConfig config) {
        if (type == StorageType.JSON) {
            Path jsonPath = resolvePath(config.getStorageJsonPath());
            return new JsonStorageProvider(jsonPath, config.getStorageJsonShardConfig());
        }

        if (type == StorageType.POSTGRESQL) {
            PostgresConfig pg = config.getPostgresConfig();
            if (!pg.isValid()) {
                logService.warn(LogCategory.STORAGE, I18n.get().tr(LangKeys.LOG_STORAGE_POSTGRES_INVALID_CONFIG));
                return null;
            }
            return new PostgresStorageProvider(pg, logService);
        }

        return null;
    }

    private void fallbackToJson(StorageType from, CoreConfig config) {
        provider = new JsonStorageProvider(resolvePath(config.getStorageJsonPath()), config.getStorageJsonShardConfig());
        try {
            provider.init();
            logService.warn(LogCategory.STORAGE, I18n.get().tr(LangKeys.LOG_STORAGE_FALLBACK, from.getId(), provider.getType().getId()));
            logService.info(LogCategory.STORAGE, I18n.get().tr(LangKeys.LOG_STORAGE_INIT_SUCCESS, provider.getType().getId()));
        } catch (Exception e) {
            logService.error(LogCategory.STORAGE, I18n.get().tr(LangKeys.LOG_STORAGE_INIT_FAILED, provider.getType().getId()), e);
        }
    }

    private Path resolvePath(String configuredPath) {
        Path path = Path.of(configuredPath);
        if (path.isAbsolute()) {
            return path;
        }
        return dataFolder.resolve(path);
    }
}
