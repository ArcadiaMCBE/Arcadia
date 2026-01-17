package Arcadia.ClexaGod.arcadia.storage.repository.json;

import Arcadia.ClexaGod.arcadia.i18n.LangKeys;
import Arcadia.ClexaGod.arcadia.logging.LogCategory;
import Arcadia.ClexaGod.arcadia.logging.LogService;
import Arcadia.ClexaGod.arcadia.storage.json.JsonShardConfig;
import Arcadia.ClexaGod.arcadia.storage.json.JsonShardStrategy;
import Arcadia.ClexaGod.arcadia.storage.json.AtomicFileWriter;
import Arcadia.ClexaGod.arcadia.storage.model.StorageRecord;
import Arcadia.ClexaGod.arcadia.storage.repository.StorageRepository;
import Arcadia.ClexaGod.arcadia.storage.retry.RetryExecutor;
import Arcadia.ClexaGod.arcadia.storage.retry.RetryOutcome;
import Arcadia.ClexaGod.arcadia.storage.retry.RetryPolicy;
import org.allaymc.api.message.I18n;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class JsonRepository<T extends StorageRecord> implements StorageRepository<T> {

    private final String name;
    private final Path rootPath;
    private final JsonCodec<T> codec;
    private final LogService logService;
    private final RetryPolicy retryPolicy;
    private final JsonShardConfig shardConfig;

    private static final ThreadLocal<MessageDigest> HASHER = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            return null;
        }
    });

    public JsonRepository(String name, Path rootPath, JsonCodec<T> codec, LogService logService) {
        this(name, rootPath, codec, logService, null, null);
    }

    public JsonRepository(String name, Path rootPath, JsonCodec<T> codec, LogService logService, RetryPolicy retryPolicy) {
        this(name, rootPath, codec, logService, retryPolicy, null);
    }

    public JsonRepository(String name, Path rootPath, JsonCodec<T> codec, LogService logService, RetryPolicy retryPolicy,
                          JsonShardConfig shardConfig) {
        this.name = name;
        this.rootPath = rootPath;
        this.codec = codec;
        this.logService = logService;
        this.retryPolicy = retryPolicy;
        this.shardConfig = shardConfig != null ? shardConfig : JsonShardConfig.disabled();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Optional<T> load(String id) {
        Path path = resolvePath(id);
        if (!Files.exists(path)) {
            if (shardConfig.isEnabled()) {
                Path legacy = resolveLegacyPath(id);
                if (Files.exists(legacy)) {
                    return readLegacyAndMaybeMigrate(id, legacy, path);
                }
            }
            return Optional.empty();
        }
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            return Optional.of(codec.decode(json));
        } catch (Exception e) {
            logService.error(LogCategory.STORAGE,
                    I18n.get().tr(LangKeys.LOG_STORAGE_JSON_READ_FAILED, name, id), e);
            return Optional.empty();
        }
    }

    @Override
    public void save(T record) {
        String id = record.getId();
        Path path = resolvePath(id);
        RetryOutcome outcome = RetryExecutor.run(retryPolicy, logService, LogCategory.STORAGE,
                "json/save " + name + "/" + id, () -> {
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
                logService.error(LogCategory.STORAGE,
                        I18n.get().tr(LangKeys.LOG_STORAGE_JSON_WRITE_FAILED, name, id), error);
            } else {
                logService.error(LogCategory.STORAGE,
                        I18n.get().tr(LangKeys.LOG_STORAGE_JSON_WRITE_FAILED, name, id));
            }
        }
    }

    @Override
    public void delete(String id) {
        Path path = resolvePath(id);
        RetryOutcome outcome = RetryExecutor.run(retryPolicy, logService, LogCategory.STORAGE,
                "json/delete " + name + "/" + id, () -> {
            try {
                boolean deleted = Files.deleteIfExists(path);
                if (!deleted && shardConfig.isEnabled()) {
                    Files.deleteIfExists(resolveLegacyPath(id));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        if (!outcome.success()) {
            Exception error = outcome.error();
            if (error != null) {
                logService.error(LogCategory.STORAGE,
                        I18n.get().tr(LangKeys.LOG_STORAGE_JSON_DELETE_FAILED, name, id), error);
            } else {
                logService.error(LogCategory.STORAGE,
                        I18n.get().tr(LangKeys.LOG_STORAGE_JSON_DELETE_FAILED, name, id));
            }
        }
    }

    @Override
    public boolean exists(String id) {
        Path path = resolvePath(id);
        if (Files.exists(path)) {
            return true;
        }
        if (shardConfig.isEnabled()) {
            return Files.exists(resolveLegacyPath(id));
        }
        return false;
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
        Set<String> seen = new HashSet<>();
        if (shardConfig.isEnabled()) {
            loadFromSharded(records, seen, limit);
        }
        if (records.size() < limit) {
            loadFromLegacy(records, seen, limit);
        }
        return records;
    }

    @Override
    public long count() {
        if (!Files.exists(rootPath)) {
            return 0;
        }
        Set<String> ids = new HashSet<>();
        if (shardConfig.isEnabled()) {
            collectShardedIds(ids);
        }
        collectLegacyIds(ids);
        return ids.size();
    }

    private Path resolvePath(String id) {
        if (!shardConfig.isEnabled()) {
            return resolveLegacyPath(id);
        }
        return resolveShardedPath(id);
    }

    private Path resolveLegacyPath(String id) {
        return rootPath.resolve(safeName(id) + ".json");
    }

    private Path resolveShardedPath(String id) {
        String key = shardKey(id);
        int depth = shardConfig.getDepth();
        int charsPerLevel = shardConfig.getCharsPerLevel();
        int required = depth * charsPerLevel;
        String padded = padRight(key.toLowerCase(Locale.ROOT), required, '0');
        Path current = rootPath;
        for (int i = 0; i < depth; i++) {
            int start = i * charsPerLevel;
            int end = start + charsPerLevel;
            String segment = padded.substring(start, end);
            current = current.resolve(segment);
        }
        return current.resolve(safeName(id) + ".json");
    }

    private boolean isJsonFile(Path path) {
        return path != null && path.getFileName().toString().endsWith(".json");
    }

    private boolean isLegacyFile(Path path) {
        Path parent = path != null ? path.getParent() : null;
        return parent != null && parent.equals(rootPath);
    }

    private Optional<T> readLegacyAndMaybeMigrate(String id, Path legacy, Path target) {
        try {
            String json = Files.readString(legacy, StandardCharsets.UTF_8);
            T record = codec.decode(json);
            if (record == null) {
                return Optional.empty();
            }
            if (shardConfig.isMigrateLegacyOnRead()) {
                try {
                    AtomicFileWriter.write(target, json);
                    Files.deleteIfExists(legacy);
                } catch (Exception e) {
                    logService.warn(LogCategory.STORAGE,
                            I18n.get().tr(LangKeys.LOG_STORAGE_JSON_SHARD_MIGRATE_FAILED, name, id));
                }
            }
            return Optional.of(record);
        } catch (Exception e) {
            logService.error(LogCategory.STORAGE,
                    I18n.get().tr(LangKeys.LOG_STORAGE_JSON_READ_FAILED, name, id), e);
            return Optional.empty();
        }
    }

    private void loadFromSharded(List<T> records, Set<String> seen, int limit) {
        int maxDepth = shardConfig.getDepth() + 1;
        try (Stream<Path> paths = Files.walk(rootPath, Math.max(1, maxDepth))) {
            var iterator = paths.filter(this::isJsonFile).iterator();
            while (iterator.hasNext() && records.size() < limit) {
                Path path = iterator.next();
                if (isLegacyFile(path)) {
                    continue;
                }
                loadRecord(path, records, seen, limit);
            }
        } catch (IOException e) {
            logService.error(LogCategory.STORAGE,
                    I18n.get().tr(LangKeys.LOG_STORAGE_JSON_READ_FAILED, name, "list"), e);
        }
    }

    private void loadFromLegacy(List<T> records, Set<String> seen, int limit) {
        try (Stream<Path> paths = Files.list(rootPath)) {
            var iterator = paths.filter(this::isJsonFile).iterator();
            while (iterator.hasNext() && records.size() < limit) {
                Path path = iterator.next();
                loadRecord(path, records, seen, limit);
            }
        } catch (IOException e) {
            logService.error(LogCategory.STORAGE,
                    I18n.get().tr(LangKeys.LOG_STORAGE_JSON_READ_FAILED, name, "list"), e);
        }
    }

    private void loadRecord(Path path, List<T> records, Set<String> seen, int limit) {
        String fileId = extractId(path);
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            T record = codec.decode(json);
            if (record == null) {
                return;
            }
            String recordId = record != null ? record.getId() : fileId;
            if (recordId == null || recordId.isBlank()) {
                recordId = fileId;
            }
            if (!seen.contains(recordId) && records.size() < limit) {
                records.add(record);
                seen.add(recordId);
            }
        } catch (Exception e) {
            logService.error(LogCategory.STORAGE,
                    I18n.get().tr(LangKeys.LOG_STORAGE_JSON_READ_FAILED, name, fileId), e);
        }
    }

    private void collectShardedIds(Set<String> ids) {
        int maxDepth = shardConfig.getDepth() + 1;
        try (Stream<Path> paths = Files.walk(rootPath, Math.max(1, maxDepth))) {
            paths.filter(this::isJsonFile)
                    .filter(path -> !isLegacyFile(path))
                    .forEach(path -> ids.add(extractId(path)));
        } catch (IOException e) {
            logService.error(LogCategory.STORAGE,
                    I18n.get().tr(LangKeys.LOG_STORAGE_JSON_READ_FAILED, name, "count"), e);
        }
    }

    private void collectLegacyIds(Set<String> ids) {
        try (Stream<Path> paths = Files.list(rootPath)) {
            paths.filter(this::isJsonFile)
                    .forEach(path -> ids.add(extractId(path)));
        } catch (IOException e) {
            logService.error(LogCategory.STORAGE,
                    I18n.get().tr(LangKeys.LOG_STORAGE_JSON_READ_FAILED, name, "count"), e);
        }
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

    private String shardKey(String id) {
        if (id == null) {
            return "0";
        }
        if (shardConfig.getStrategy() == JsonShardStrategy.PREFIX) {
            return safeName(id);
        }
        MessageDigest digest = HASHER.get();
        if (digest == null) {
            return Integer.toHexString(id.hashCode());
        }
        byte[] hashed = digest.digest(id.getBytes(StandardCharsets.UTF_8));
        return toHex(hashed);
    }

    private String padRight(String input, int length, char fill) {
        if (input.length() >= length) {
            return input;
        }
        StringBuilder out = new StringBuilder(length);
        out.append(input);
        while (out.length() < length) {
            out.append(fill);
        }
        return out.toString();
    }

    private String toHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        char[] hex = "0123456789abcdef".toCharArray();
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = hex[v >>> 4];
            out[i * 2 + 1] = hex[v & 0x0F];
        }
        return new String(out);
    }
}
