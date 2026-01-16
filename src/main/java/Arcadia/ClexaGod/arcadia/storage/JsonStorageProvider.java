package Arcadia.ClexaGod.arcadia.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class JsonStorageProvider implements StorageProvider {

    private final Path rootPath;
    private boolean ready;

    public JsonStorageProvider(Path rootPath) {
        this.rootPath = rootPath;
    }

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

    public Path getRootPath() {
        return rootPath;
    }
}
