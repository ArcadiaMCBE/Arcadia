package Arcadia.ClexaGod.arcadia.storage.repository.json;

import Arcadia.ClexaGod.arcadia.i18n.LangKeys;
import Arcadia.ClexaGod.arcadia.storage.json.AtomicFileWriter;
import Arcadia.ClexaGod.arcadia.storage.model.StorageRecord;
import Arcadia.ClexaGod.arcadia.storage.repository.StorageRepository;
import lombok.RequiredArgsConstructor;
import org.allaymc.api.message.I18n;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@RequiredArgsConstructor
public class JsonRepository<T extends StorageRecord> implements StorageRepository<T> {

    private final String name;
    private final Path rootPath;
    private final JsonCodec<T> codec;
    private final Logger logger;

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
        try {
            String json = codec.encode(record);
            AtomicFileWriter.write(path, json);
        } catch (Exception e) {
            logger.error(I18n.get().tr(LangKeys.LOG_STORAGE_JSON_WRITE_FAILED, name, id), e);
        }
    }

    @Override
    public void delete(String id) {
        Path path = resolvePath(id);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            logger.error(I18n.get().tr(LangKeys.LOG_STORAGE_JSON_DELETE_FAILED, name, id), e);
        }
    }

    @Override
    public boolean exists(String id) {
        return Files.exists(resolvePath(id));
    }

    private Path resolvePath(String id) {
        return rootPath.resolve(safeName(id) + ".json");
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
