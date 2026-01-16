package Arcadia.ClexaGod.arcadia.storage;

import Arcadia.ClexaGod.arcadia.config.CoreConfig;
import Arcadia.ClexaGod.arcadia.i18n.LangKeys;
import org.allaymc.api.message.I18n;
import org.slf4j.Logger;

import java.nio.file.Path;

public final class StorageManager {

    private final Logger logger;
    private final Path dataFolder;
    private StorageProvider provider;

    public StorageManager(Logger logger, Path dataFolder) {
        this.logger = logger;
        this.dataFolder = dataFolder;
    }

    public void init(CoreConfig config) {
        StorageType requested = config.getStorageType();
        logger.info(I18n.get().tr(LangKeys.LOG_STORAGE_SELECTED, requested.getId()));

        provider = createProvider(requested, config);
        if (provider == null) {
            fallbackToJson(requested, config);
            return;
        }

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

    public void close() {
        if (provider != null) {
            provider.close();
        }
    }

    public StorageProvider getProvider() {
        return provider;
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
