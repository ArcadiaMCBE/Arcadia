package Arcadia.ClexaGod.arcadia.storage;

import Arcadia.ClexaGod.arcadia.config.CoreConfig;
import Arcadia.ClexaGod.arcadia.i18n.LangKeys;
import Arcadia.ClexaGod.arcadia.storage.cache.StorageCacheManager;
import Arcadia.ClexaGod.arcadia.storage.queue.AsyncWriteQueue;
import Arcadia.ClexaGod.arcadia.storage.model.StorageRecord;
import Arcadia.ClexaGod.arcadia.storage.repository.StorageRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.allaymc.api.message.I18n;
import org.allaymc.api.scheduler.TaskCreator;
import org.allaymc.api.server.Server;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.time.Duration;

@RequiredArgsConstructor
public final class StorageManager {

    private final Logger logger;
    private final Path dataFolder;
    private final TaskCreator taskCreator;
    @Getter
    private AsyncWriteQueue writeQueue;
    private StorageProvider provider;
    @Getter
    private StorageCacheManager cacheManager;

    public void init(CoreConfig config) {
        if (writeQueue == null) {
            writeQueue = new AsyncWriteQueue(Server.getInstance().getVirtualThreadPool(), logger, 5000);
            writeQueue.start();
        }
        StorageType requested = config.getStorageType();
        logger.info(I18n.get().tr(LangKeys.LOG_STORAGE_SELECTED, requested.getId()));

        provider = createProvider(requested, config);
        if (provider == null) {
            fallbackToJson(requested, config);
        } else {
            try {
                provider.init();
                logger.info(I18n.get().tr(LangKeys.LOG_STORAGE_INIT_SUCCESS, provider.getType().getId()));
            } catch (Exception e) {
                logger.error(I18n.get().tr(LangKeys.LOG_STORAGE_INIT_FAILED, provider.getType().getId()), e);
                if (requested != StorageType.JSON) {
                    fallbackToJson(requested, config);
                }
            }
        }

        if (cacheManager == null) {
            cacheManager = new StorageCacheManager(
                    config.getCacheConfig(),
                    logger,
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
        }
        if (writeQueue != null && writeQueue.isStarted()) {
            writeQueue.shutdown(Duration.ofSeconds(10));
        }
        if (provider != null) {
            provider.close();
        }
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

    private StorageProvider createProvider(StorageType type, CoreConfig config) {
        if (type == StorageType.JSON) {
            Path jsonPath = resolvePath(config.getStorageJsonPath());
            return new JsonStorageProvider(jsonPath);
        }

        if (type == StorageType.POSTGRESQL) {
            PostgresConfig pg = config.getPostgresConfig();
            if (!pg.isValid()) {
                logger.warn(I18n.get().tr(LangKeys.LOG_STORAGE_POSTGRES_INVALID_CONFIG));
                return null;
            }
            return new PostgresStorageProvider(pg, logger);
        }

        return null;
    }

    private void fallbackToJson(StorageType from, CoreConfig config) {
        provider = new JsonStorageProvider(resolvePath(config.getStorageJsonPath()));
        try {
            provider.init();
            logger.warn(I18n.get().tr(LangKeys.LOG_STORAGE_FALLBACK, from.getId(), provider.getType().getId()));
            logger.info(I18n.get().tr(LangKeys.LOG_STORAGE_INIT_SUCCESS, provider.getType().getId()));
        } catch (Exception e) {
            logger.error(I18n.get().tr(LangKeys.LOG_STORAGE_INIT_FAILED, provider.getType().getId()), e);
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
