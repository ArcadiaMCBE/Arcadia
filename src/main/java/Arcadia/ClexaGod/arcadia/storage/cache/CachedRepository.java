package Arcadia.ClexaGod.arcadia.storage.cache;

import Arcadia.ClexaGod.arcadia.i18n.LangKeys;
import Arcadia.ClexaGod.arcadia.storage.model.StorageRecord;
import Arcadia.ClexaGod.arcadia.storage.queue.AsyncWriteQueue;
import Arcadia.ClexaGod.arcadia.storage.queue.WriteTask;
import Arcadia.ClexaGod.arcadia.storage.repository.StorageRepository;
import org.allaymc.api.message.I18n;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CachedRepository<T extends StorageRecord> implements StorageRepository<T> {

    private final StorageRepository<T> delegate;
    private final RecordCache<T> cache;
    private final AsyncWriteQueue queue;
    private final Logger logger;
    private final CachePolicy policy;

    public CachedRepository(StorageRepository<T> delegate, RecordCache<T> cache, AsyncWriteQueue queue, Logger logger,
                            CachePolicy policy) {
        this.delegate = delegate;
        this.cache = cache;
        this.queue = queue;
        this.logger = logger;
        this.policy = policy;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public Optional<T> load(String id) {
        Optional<T> cached = cache.get(id);
        if (cached.isPresent()) {
            return cached;
        }
        Optional<T> loaded = delegate.load(id);
        loaded.ifPresent(record -> cache.put(record, false));
        return loaded;
    }

    @Override
    public void save(T record) {
        if (record == null) {
            return;
        }
        cache.put(record, true);
        if (policy.flushOnSave()) {
            boolean flushed = enqueueSaveAndWait(record);
            if (flushed) {
                cache.markClean(record.getId());
            } else {
                logger.warn(I18n.get().tr(LangKeys.LOG_STORAGE_CACHE_FLUSH_ON_SAVE_FAILED, getName(), record.getId()));
            }
            return;
        }
        enqueueSave(record);
    }

    @Override
    public void delete(String id) {
        cache.remove(id);
        enqueueDelete(id);
    }

    @Override
    public boolean exists(String id) {
        if (cache.contains(id)) {
            return true;
        }
        return delegate.exists(id);
    }

    @Override
    public List<T> loadAll() {
        return loadAll(Integer.MAX_VALUE);
    }

    @Override
    public List<T> loadAll(int limit) {
        if (limit <= 0) {
            return List.of();
        }
        cache.evictExpired();
        List<T> base = delegate.loadAll(limit);
        Map<String, T> merged = new LinkedHashMap<>(Math.min(limit, base.size()));
        for (T record : base) {
            if (record == null) {
                continue;
            }
            merged.put(record.getId(), record);
            cache.put(record, false);
            if (merged.size() >= limit) {
                break;
            }
        }
        for (RecordCache.CacheSnapshot<T> snapshot : cache.snapshotAll()) {
            if (merged.containsKey(snapshot.id())) {
                merged.put(snapshot.id(), snapshot.value());
                continue;
            }
            if (merged.size() >= limit) {
                break;
            }
            merged.put(snapshot.id(), snapshot.value());
        }
        return new ArrayList<>(merged.values());
    }

    @Override
    public long count() {
        return loadAll().size();
    }

    public void flush() {
        int expired = cache.evictExpired();
        int evicted = cache.evictOverflow();

        List<RecordCache.CacheSnapshot<T>> dirty = cache.snapshotDirty();
        int queued = 0;
        for (RecordCache.CacheSnapshot<T> snapshot : dirty) {
            if (enqueueSave(snapshot.value())) {
                queued++;
            }
        }

        int remaining = cache.countDirty();
        if (queued > 0 || expired > 0 || evicted > 0 || remaining > 0) {
            logger.info(I18n.get().tr(
                    LangKeys.LOG_STORAGE_CACHE_FLUSH,
                    getName(), queued, remaining, evicted, expired
            ));
        }

        int size = cache.size();
        if (size > cache.getMaxEntries()) {
            logger.warn(I18n.get().tr(LangKeys.LOG_STORAGE_CACHE_OVERFLOW, getName(), size, cache.getMaxEntries()));
        }
    }

    private void enqueueDelete(String id) {
        String key = buildKey(id);
        if (key == null) {
            return;
        }
        queue.enqueue(new WriteTask(key, "delete " + getName() + "/" + id, () -> delegate.delete(id)));
    }

    private boolean enqueueSave(T record) {
        String id = record.getId();
        String key = buildKey(id);
        if (key == null) {
            return false;
        }
        boolean enqueued = queue.enqueue(new WriteTask(key, "save " + getName() + "/" + id, () -> delegate.save(record)));
        if (enqueued) {
            cache.markClean(id);
        }
        return enqueued;
    }

    private boolean enqueueSaveAndWait(T record) {
        String id = record.getId();
        String key = buildKey(id);
        if (key == null) {
            return false;
        }
        WriteTask task = new WriteTask(key, "save " + getName() + "/" + id, () -> delegate.save(record));
        return queue.enqueueAndWait(task, policy.flushTimeout());
    }

    private String buildKey(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return getName() + ":" + id.trim();
    }
}
