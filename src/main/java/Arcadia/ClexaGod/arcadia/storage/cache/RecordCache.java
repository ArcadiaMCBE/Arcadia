package Arcadia.ClexaGod.arcadia.storage.cache;

import Arcadia.ClexaGod.arcadia.storage.model.StorageRecord;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RecordCache<T extends StorageRecord> {

    private final int maxEntries;
    private final long ttlMillis;
    private final LinkedHashMap<String, CacheEntry<T>> entries = new LinkedHashMap<>(16, 0.75f, true);

    public RecordCache(int maxEntries, int ttlSeconds) {
        this.maxEntries = Math.max(1, maxEntries);
        this.ttlMillis = Math.max(1, ttlSeconds) * 1000L;
    }

    public Optional<T> get(String id) {
        String key = normalizeId(id);
        if (key == null) {
            return Optional.empty();
        }
        long now = System.currentTimeMillis();
        synchronized (entries) {
            CacheEntry<T> entry = entries.get(key);
            if (entry == null) {
                return Optional.empty();
            }
            if (isExpired(entry, now) && !entry.dirty) {
                entries.remove(key);
                return Optional.empty();
            }
            entry.lastAccess = now;
            return Optional.of(entry.value);
        }
    }

    public boolean contains(String id) {
        String key = normalizeId(id);
        if (key == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        synchronized (entries) {
            CacheEntry<T> entry = entries.get(key);
            if (entry == null) {
                return false;
            }
            if (isExpired(entry, now) && !entry.dirty) {
                entries.remove(key);
                return false;
            }
            entry.lastAccess = now;
            return true;
        }
    }

    public void put(T record, boolean dirty) {
        if (record == null) {
            return;
        }
        String key = normalizeId(record.getId());
        if (key == null) {
            return;
        }
        long now = System.currentTimeMillis();
        synchronized (entries) {
            CacheEntry<T> existing = entries.get(key);
            if (existing == null) {
                entries.put(key, new CacheEntry<>(record, dirty, now));
            } else {
                existing.value = record;
                existing.lastAccess = now;
                existing.dirty = existing.dirty || dirty;
            }
        }
    }

    public void remove(String id) {
        String key = normalizeId(id);
        if (key == null) {
            return;
        }
        synchronized (entries) {
            entries.remove(key);
        }
    }

    public void markClean(String id) {
        String key = normalizeId(id);
        if (key == null) {
            return;
        }
        synchronized (entries) {
            CacheEntry<T> entry = entries.get(key);
            if (entry != null) {
                entry.dirty = false;
            }
        }
    }

    public List<CacheSnapshot<T>> snapshotDirty() {
        synchronized (entries) {
            List<CacheSnapshot<T>> dirty = new ArrayList<>();
            for (Map.Entry<String, CacheEntry<T>> entry : entries.entrySet()) {
                CacheEntry<T> value = entry.getValue();
                if (value.dirty) {
                    dirty.add(new CacheSnapshot<>(entry.getKey(), value.value));
                }
            }
            return dirty;
        }
    }

    public int countDirty() {
        int count = 0;
        synchronized (entries) {
            for (CacheEntry<T> entry : entries.values()) {
                if (entry.dirty) {
                    count++;
                }
            }
        }
        return count;
    }

    public int evictExpired() {
        long now = System.currentTimeMillis();
        int removed = 0;
        synchronized (entries) {
            var iterator = entries.entrySet().iterator();
            while (iterator.hasNext()) {
                CacheEntry<T> entry = iterator.next().getValue();
                if (!entry.dirty && isExpired(entry, now)) {
                    iterator.remove();
                    removed++;
                }
            }
        }
        return removed;
    }

    public int evictOverflow() {
        int removed = 0;
        synchronized (entries) {
            if (entries.size() <= maxEntries) {
                return 0;
            }
            var iterator = entries.entrySet().iterator();
            while (entries.size() > maxEntries && iterator.hasNext()) {
                CacheEntry<T> entry = iterator.next().getValue();
                if (entry.dirty) {
                    continue;
                }
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }

    public int size() {
        synchronized (entries) {
            return entries.size();
        }
    }

    public int getMaxEntries() {
        return maxEntries;
    }

    private boolean isExpired(CacheEntry<T> entry, long now) {
        return now - entry.lastAccess >= ttlMillis;
    }

    private String normalizeId(String id) {
        if (id == null) {
            return null;
        }
        String trimmed = id.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static final class CacheEntry<T> {
        private T value;
        private boolean dirty;
        private long lastAccess;

        private CacheEntry(T value, boolean dirty, long lastAccess) {
            this.value = value;
            this.dirty = dirty;
            this.lastAccess = lastAccess;
        }
    }

    public record CacheSnapshot<T>(String id, T value) {
    }
}
