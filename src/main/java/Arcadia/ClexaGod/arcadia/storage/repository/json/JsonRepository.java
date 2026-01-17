package Arcadia.ClexaGod.arcadia.storage.repository.json;

import Arcadia.ClexaGod.arcadia.i18n.LangKeys;
import Arcadia.ClexaGod.arcadia.storage.json.AtomicFileWriter;
import Arcadia.ClexaGod.arcadia.storage.model.StorageRecord;
import Arcadia.ClexaGod.arcadia.storage.repository.StorageRepository;
import Arcadia.ClexaGod.arcadia.storage.retry.RetryExecutor;
import Arcadia.ClexaGod.arcadia.storage.retry.RetryOutcome;
import Arcadia.ClexaGod.arcadia.storage.retry.RetryPolicy;
import org.allaymc.api.message.I18n;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class JsonRepository<T extends StorageRecord> implements StorageRepository<T> {

    private final String name;
    private final Path rootPath;
    private final JsonCodec<T> codec;
    private final Logger logger;
    private final RetryPolicy retryPolicy;

    public JsonRepository(String name, Path rootPath, JsonCodec<T> codec, Logger logger) {
        this(name, rootPath, codec, logger, null);
    }

    public JsonRepository(String name, Path rootPath, JsonCodec<T> codec, Logger logger, RetryPolicy retryPolicy) {
        this.name = name;
        this.rootPath = rootPath;
        this.codec = codec;
        this.logger = logger;
        this.retryPolicy = retryPolicy;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Optional<T> load(String id) {
        Path path = resolvePath(id);
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            return Optional.of(codec.decode(json));
        } catch (Exception e) {
            logger.error(I18n.get().tr(LangKeys.LOG_STORAGE_JSON_READ_FAILED, name, id), e);
            return Optional.empty();
        }
    }

    @Override
    public void save(T record) {
        String id = record.getId();
        Path path = resolvePath(id);
        RetryOutcome outcome = RetryExecutor.run(retryPolicy, logger, "json/save " + name + "/" + id, () -> {
            try {
                String json = codec.encode(record);
                AtomicFileWriter.write(path, json);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        if (!outcome.success()) {
            Exception error = outcome.error();
            if (error != null) {
                logger.error(I18n.get().tr(LangKeys.LOG_STORAGE_JSON_WRITE_FAILED, name, id), error);
            } else {
                logger.error(I18n.get().tr(LangKeys.LOG_STORAGE_JSON_WRITE_FAILED, name, id));
            }
        }
    }

    @Override
    public void delete(String id) {
        Path path = resolvePath(id);
        RetryOutcome outcome = RetryExecutor.run(retryPolicy, logger, "json/delete " + name + "/" + id, () -> {
            try {
                Files.deleteIfExists(path);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        if (!outcome.success()) {
            Exception error = outcome.error();
            if (error != null) {
                logger.error(I18n.get().tr(LangKeys.LOG_STORAGE_JSON_DELETE_FAILED, name, id), error);
            } else {
                logger.error(I18n.get().tr(LangKeys.LOG_STORAGE_JSON_DELETE_FAILED, name, id));
            }
        }
    }

    @Override
    public boolean exists(String id) {
        return Files.exists(resolvePath(id));
    }

    @Override
    public List<T> loadAll() {
        return loadAll(Integer.MAX_VALUE);
    }

    @Override
    public List<T> loadAll(int limit) {
        if (limit <= 0 || !Files.exists(rootPath)) {
            return List.of();
        }
        List<T> records = new ArrayList<>();
        try (Stream<Path> paths = Files.list(rootPath)) {
            var iterator = paths.filter(this::isJsonFile).iterator();
            while (iterator.hasNext() && records.size() < limit) {
                Path path = iterator.next();
                String id = extractId(path);
                try {
                    String json = Files.readString(path, StandardCharsets.UTF_8);
                    records.add(codec.decode(json));
                } catch (Exception e) {
                    logger.error(I18n.get().tr(LangKeys.LOG_STORAGE_JSON_READ_FAILED, name, id), e);
                }
            }
        } catch (IOException e) {
            logger.error(I18n.get().tr(LangKeys.LOG_STORAGE_JSON_READ_FAILED, name, "list"), e);
        }
        return records;
    }

    @Override
    public long count() {
        if (!Files.exists(rootPath)) {
            return 0;
        }
        try (Stream<Path> paths = Files.list(rootPath)) {
            return paths.filter(this::isJsonFile).count();
        } catch (IOException e) {
            logger.error(I18n.get().tr(LangKeys.LOG_STORAGE_JSON_READ_FAILED, name, "count"), e);
            return 0;
        }
    }

    private Path resolvePath(String id) {
        return rootPath.resolve(safeName(id) + ".json");
    }

    private boolean isJsonFile(Path path) {
        return path != null && path.getFileName().toString().endsWith(".json");
    }

    private String extractId(Path path) {
        String fileName = path.getFileName().toString();
        int idx = fileName.lastIndexOf('.');
        if (idx > 0) {
            return fileName.substring(0, idx);
        }
        return fileName;
    }

    private String safeName(String id) {
        if (id == null) {
            return "unknown";
        }
        String trimmed = id.trim();
        if (trimmed.isEmpty()) {
            return "unknown";
        }
        StringBuilder out = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == '.') {
                out.append(c);
            } else {
                out.append('_');
            }
        }
        return out.toString();
    }
}
