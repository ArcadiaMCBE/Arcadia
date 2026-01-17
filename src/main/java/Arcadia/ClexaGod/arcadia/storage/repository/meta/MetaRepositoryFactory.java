package Arcadia.ClexaGod.arcadia.storage.repository.meta;

import Arcadia.ClexaGod.arcadia.storage.JsonStorageProvider;
import Arcadia.ClexaGod.arcadia.storage.PostgresStorageProvider;
import Arcadia.ClexaGod.arcadia.storage.StorageManager;
import Arcadia.ClexaGod.arcadia.storage.StorageProvider;
import Arcadia.ClexaGod.arcadia.storage.repository.StorageRepository;
import Arcadia.ClexaGod.arcadia.storage.repository.json.JsonRepository;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Optional;

public final class MetaRepositoryFactory {

    private MetaRepositoryFactory() {
    }

    public static Optional<StorageRepository<MetaRecord>> create(StorageManager storageManager, Logger logger) {
        StorageProvider provider = storageManager.getProvider();
        if (provider instanceof JsonStorageProvider jsonProvider) {
            Path metaPath = jsonProvider.getRootPath().resolve("meta");
            StorageRepository<MetaRecord> base = new JsonRepository<>(
                    "meta",
                    metaPath,
                    new MetaJsonCodec(),
                    logger,
                    storageManager.getRetryPolicy()
            );
            return Optional.of(storageManager.withCache(base));
        }

        if (provider instanceof PostgresStorageProvider pgProvider) {
            StorageRepository<MetaRecord> base = new PostgresMetaRepository(
                    pgProvider.getDataSource(),
                    logger,
                    storageManager.getRetryPolicy()
            );
            return Optional.of(storageManager.withCache(base));
        }

        return Optional.empty();
    }
}
