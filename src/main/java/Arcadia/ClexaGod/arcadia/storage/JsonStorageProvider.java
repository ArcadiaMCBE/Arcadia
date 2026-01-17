package Arcadia.ClexaGod.arcadia.storage;

import Arcadia.ClexaGod.arcadia.storage.json.JsonShardConfig;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RequiredArgsConstructor
public final class JsonStorageProvider implements StorageProvider {

    @Getter
    private final Path rootPath;
    @Getter
    private final JsonShardConfig shardConfig;
    private boolean ready;

    @Override
    public StorageType getType() {
        return StorageType.JSON;
    }

    @Override
    public void init() throws IOException {
        if (!Files.exists(rootPath)) {
            Files.createDirectories(rootPath);
        }
        ready = true;
    }

    @Override
    public void close() {
        ready = false;
    }

    @Override
    public boolean isReady() {
        return ready;
    }

}
