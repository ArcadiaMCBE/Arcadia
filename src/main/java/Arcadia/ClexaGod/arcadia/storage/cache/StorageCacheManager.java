package Arcadia.ClexaGod.arcadia.storage.cache;

import Arcadia.ClexaGod.arcadia.i18n.LangKeys;
import Arcadia.ClexaGod.arcadia.logging.LogCategory;
import Arcadia.ClexaGod.arcadia.logging.LogService;
import Arcadia.ClexaGod.arcadia.storage.model.StorageRecord;
import Arcadia.ClexaGod.arcadia.storage.queue.AsyncWriteQueue;
import Arcadia.ClexaGod.arcadia.storage.repository.StorageRepository;
import org.allaymc.api.message.I18n;
import org.allaymc.api.scheduler.Scheduler;
import org.allaymc.api.scheduler.TaskCreator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public final class StorageCacheManager {

    private final CacheConfig config;
    private final LogService logService;
    private final AsyncWriteQueue writeQueue;
    private final Scheduler scheduler;
    private final TaskCreator taskCreator;
    private final List<CachedRepository<?>> repositories = new CopyOnWriteArrayList<>();
    private volatile boolean started;
    private final AtomicBoolean warmupScheduled = new AtomicBoolean(false);

    public StorageCacheManager(CacheConfig config, LogService logService, AsyncWriteQueue writeQueue,
                               Scheduler scheduler, TaskCreator taskCreator) {
        this.config = Objects.requireNonNull(config, "config");
        this.logService = Objects.requireNonNull(logService, "logService");
        this.writeQueue = Objects.requireNonNull(writeQueue, "writeQueue");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.taskCreator = Objects.requireNonNull(taskCreator, "taskCreator");
    }

    public boolean isEnabled() {
        return config.isEnabled();
    }

    public void start() {
        if (!config.isEnabled()) {
            logService.info(LogCategory.CACHE, I18n.get().tr(LangKeys.LOG_STORAGE_CACHE_DISABLED));
            return;
        }
        if (started) {
            return;
        }
        started = true;
        int flushTicks = Math.max(1, config.getFlushIntervalSeconds()) * 20;
        scheduler.scheduleRepeating(taskCreator, this::flushAll, flushTicks, true);
        logService.info(LogCategory.CACHE, I18n.get().tr(
                LangKeys.LOG_STORAGE_CACHE_ENABLED,
                config.getTtlSeconds(), config.getMaxEntries(), config.getFlushIntervalSeconds()
        ));
    }

    public void shutdown() {
        if (!started) {
            return;
        }
        flushAll();
        started = false;
    }

    public boolean isFlushOnPlayerQuit() {
        return config.isFlushOnPlayerQuit();
    }

    public <T extends StorageRecord> StorageRepository<T> wrap(StorageRepository<T> repository) {
        if (!config.isEnabled()) {
            return repository;
        }
        CachePolicy policy = config.resolvePolicy(repository.getName());
        return wrap(repository, policy);
    }

    public <T extends StorageRecord> StorageRepository<T> wrap(StorageRepository<T> repository, CachePolicy policy) {
        if (!config.isEnabled()) {
            return repository;
        }
        CachePolicy effective = policy != null ? policy : CachePolicy.defaultPolicy();
        if (!effective.enabled()) {
            return repository;
        }
        RecordCache<T> cache = new RecordCache<>(config.getMaxEntries(), config.getTtlSeconds());
        CachedRepository<T> cached = new CachedRepository<>(repository, cache, writeQueue, logService, effective);
        repositories.add(cached);
        return cached;
    }

    public void flushAll() {
        if (!started) {
            return;
        }
        for (CachedRepository<?> repository : repositories) {
            repository.flush();
        }
    }

    public void warmUp() {
        if (!started || !config.isWarmupEnabled()) {
            return;
        }
        if (!warmupScheduled.compareAndSet(false, true)) {
            return;
        }
        int delayTicks = Math.max(0, config.getWarmupDelaySeconds()) * 20;
        scheduler.scheduleDelayed(taskCreator, this::runWarmUp, delayTicks, true);
    }

    private void runWarmUp() {
        if (!started || !config.isWarmupEnabled()) {
            return;
        }
        int limit = config.getWarmupMaxEntries();
        logService.info(LogCategory.CACHE, I18n.get().tr(LangKeys.LOG_STORAGE_CACHE_WARMUP_START, limit));
        int loaded = 0;
        for (CachedRepository<?> repository : repositories) {
            loaded += repository.loadAll(limit).size();
        }
        logService.info(LogCategory.CACHE,
                I18n.get().tr(LangKeys.LOG_STORAGE_CACHE_WARMUP_COMPLETE, repositories.size(), loaded));
    }

    public CacheMetrics.CacheMetricsSnapshot snapshotMetrics() {
        long hits = 0;
        long misses = 0;
        long flushes = 0;
        long writeTasks = 0;
        long evictedExpired = 0;
        long evictedOverflow = 0;
        int lastQueueSize = 0;
        int maxQueueSize = 0;
        for (CachedRepository<?> repository : repositories) {
            CacheMetrics.CacheMetricsSnapshot snapshot = repository.getMetrics().snapshot(repository.getName());
            hits += snapshot.hits();
            misses += snapshot.misses();
            flushes += snapshot.flushes();
            writeTasks += snapshot.writeTasks();
            evictedExpired += snapshot.evictedExpired();
            evictedOverflow += snapshot.evictedOverflow();
            lastQueueSize = Math.max(lastQueueSize, snapshot.lastQueueSize());
            maxQueueSize = Math.max(maxQueueSize, snapshot.maxQueueSize());
        }
        double hitRate = hits + misses == 0 ? 0.0 : (double) hits / (double) (hits + misses);
        return new CacheMetrics.CacheMetricsSnapshot(
                "all",
                hits,
                misses,
                hitRate,
                flushes,
                writeTasks,
                evictedExpired,
                evictedOverflow,
                lastQueueSize,
                maxQueueSize
        );
    }

    public List<CacheMetrics.CacheMetricsSnapshot> snapshotMetricsPerRepo() {
        List<CacheMetrics.CacheMetricsSnapshot> snapshots = new ArrayList<>();
        for (CachedRepository<?> repository : repositories) {
            snapshots.add(repository.getMetrics().snapshot(repository.getName()));
        }
        return snapshots;
    }
}
