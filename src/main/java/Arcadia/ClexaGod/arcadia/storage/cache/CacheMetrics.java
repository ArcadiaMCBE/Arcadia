package Arcadia.ClexaGod.arcadia.storage.cache;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

public final class CacheMetrics {

    private final LongAdder hits = new LongAdder();
    private final LongAdder misses = new LongAdder();
    private final LongAdder flushes = new LongAdder();
    private final LongAdder writeTasks = new LongAdder();
    private final LongAdder evictedExpired = new LongAdder();
    private final LongAdder evictedOverflow = new LongAdder();
    private final AtomicInteger lastQueueSize = new AtomicInteger();
    private final AtomicInteger maxQueueSize = new AtomicInteger();

    public void recordHit() {
        hits.increment();
    }

    public void recordMiss() {
        misses.increment();
    }

    public void recordFlush() {
        flushes.increment();
    }

    public void recordWriteTasks(int count) {
        if (count > 0) {
            writeTasks.add(count);
        }
    }

    public void recordEvictedExpired(int count) {
        if (count > 0) {
            evictedExpired.add(count);
        }
    }

    public void recordEvictedOverflow(int count) {
        if (count > 0) {
            evictedOverflow.add(count);
        }
    }

    public void recordQueueSize(int size) {
        if (size < 0) {
            return;
        }
        lastQueueSize.set(size);
        maxQueueSize.updateAndGet(current -> Math.max(current, size));
    }

    public CacheMetricsSnapshot snapshot(String name) {
        long hitValue = hits.sum();
        long missValue = misses.sum();
        long total = hitValue + missValue;
        double hitRate = total == 0 ? 0.0 : (double) hitValue / (double) total;
        return new CacheMetricsSnapshot(
                name,
                hitValue,
                missValue,
                hitRate,
                flushes.sum(),
                writeTasks.sum(),
                evictedExpired.sum(),
                evictedOverflow.sum(),
                lastQueueSize.get(),
                maxQueueSize.get()
        );
    }

    public record CacheMetricsSnapshot(
            String name,
            long hits,
            long misses,
            double hitRate,
            long flushes,
            long writeTasks,
            long evictedExpired,
            long evictedOverflow,
            int lastQueueSize,
            int maxQueueSize
    ) {
    }
}
