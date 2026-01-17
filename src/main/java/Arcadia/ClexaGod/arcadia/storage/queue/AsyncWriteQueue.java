package Arcadia.ClexaGod.arcadia.storage.queue;

import Arcadia.ClexaGod.arcadia.i18n.LangKeys;
import Arcadia.ClexaGod.arcadia.logging.LogCategory;
import Arcadia.ClexaGod.arcadia.logging.LogService;
import lombok.Getter;
import org.allaymc.api.message.I18n;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AsyncWriteQueue {

    private final ExecutorService executor;
    private final LogService logService;
    private final int maxQueueSize;
    private final QueueFullPolicy fullPolicy;
    private final int fullTimeoutMs;
    private final BlockingQueue<QueueItem> queue;
    private final ConcurrentMap<String, KeyedTask> keyedTasks = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean accepting = new AtomicBoolean(false);
    private Future<?> workerFuture;

    @Getter
    private volatile boolean started;

    public AsyncWriteQueue(ExecutorService executor, LogService logService, int maxQueueSize, QueueFullPolicy fullPolicy, int fullTimeoutMs) {
        this.executor = Objects.requireNonNull(executor, "executor");
        this.logService = Objects.requireNonNull(logService, "logService");
        this.maxQueueSize = Math.max(1, maxQueueSize);
        this.fullPolicy = fullPolicy != null ? fullPolicy : QueueFullPolicy.BLOCK;
        this.fullTimeoutMs = Math.max(0, fullTimeoutMs);
        this.queue = new LinkedBlockingQueue<>(this.maxQueueSize);
    }

    public int getMaxQueueSize() {
        return maxQueueSize;
    }

    public QueueFullPolicy getFullPolicy() {
        return fullPolicy;
    }

    public int getFullTimeoutMs() {
        return fullTimeoutMs;
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        accepting.set(true);
        workerFuture = executor.submit(this::runLoop);
        started = true;
        logService.info(LogCategory.QUEUE, I18n.get().tr(LangKeys.LOG_STORAGE_QUEUE_STARTED));
    }

    public boolean enqueue(WriteTask task) {
        if (task == null) {
            return false;
        }
        if (!accepting.get()) {
            return false;
        }

        String key = normalizeKey(task.key());
        if (key == null) {
            OfferResult result = offerQueueItem(new QueueItem(null, task), task, null);
            return result != OfferResult.DROPPED;
        }

        KeyedTask next = new KeyedTask(task, new CompletableFuture<>());
        KeyedTask previous = keyedTasks.put(key, next);
        if (previous == null) {
            OfferResult result = offerQueueItem(new QueueItem(key, null), task, next.completion());
            if (result != OfferResult.QUEUED) {
                keyedTasks.remove(key, next);
            }
            return result != OfferResult.DROPPED;
        }
        previous.completion().complete(false);
        return true;
    }

    public boolean enqueueAndWait(WriteTask task, Duration timeout) {
        if (task == null || timeout == null) {
            return false;
        }
        if (!accepting.get()) {
            return false;
        }
        String key = normalizeKey(task.key());
        if (key == null) {
            return enqueueAndWaitUnkeyed(task, timeout);
        }

        CompletableFuture<Boolean> completion = new CompletableFuture<>();
        KeyedTask next = new KeyedTask(task, completion);
        KeyedTask previous = keyedTasks.put(key, next);
        if (previous == null) {
            OfferResult result = offerQueueItem(new QueueItem(key, null), task, completion);
            if (result != OfferResult.QUEUED) {
                keyedTasks.remove(key, next);
                if (result == OfferResult.DROPPED) {
                    return false;
                }
            }
        } else {
            previous.completion().complete(false);
        }
        return awaitCompletion(completion, timeout);
    }

    private boolean enqueueAndWaitUnkeyed(WriteTask task, Duration timeout) {
        CompletableFuture<Boolean> completion = new CompletableFuture<>();
        WriteTask wrapped = new WriteTask(null, task.description(), () -> {
            try {
                task.action().run();
                completion.complete(true);
            } catch (Exception e) {
                completion.completeExceptionally(e);
                throw e;
            }
        });
        OfferResult result = offerQueueItem(new QueueItem(null, wrapped), task, completion);
        if (result == OfferResult.DROPPED) {
            return false;
        }
        return awaitCompletion(completion, timeout);
    }

    public void shutdown(Duration timeout) {
        accepting.set(false);
        running.set(false);
        logService.info(LogCategory.QUEUE, I18n.get().tr(LangKeys.LOG_STORAGE_QUEUE_STOPPING));
        boolean drained = waitForDrain(timeout);
        if (!drained) {
            int pendingItems = queue.size();
            int pendingKeys = keyedTasks.size();
            logService.warn(LogCategory.QUEUE,
                    I18n.get().tr(LangKeys.LOG_STORAGE_QUEUE_FORCE_DRAIN, pendingItems, pendingKeys));
            cancelWorker();
            int drainedInline = drainInline();
            logService.warn(LogCategory.QUEUE,
                    I18n.get().tr(LangKeys.LOG_STORAGE_QUEUE_FORCE_DRAIN_COMPLETE, drainedInline));
        }
        started = false;
        logService.info(LogCategory.QUEUE, I18n.get().tr(LangKeys.LOG_STORAGE_QUEUE_STOPPED));
    }

    public int getQueueSize() {
        return queue.size();
    }

    private void runLoop() {
        while (running.get() || !queue.isEmpty()) {
            QueueItem item;
            try {
                item = queue.poll(200, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            if (item == null) {
                continue;
            }
            ResolvedTask resolved = resolveTask(item);
            if (resolved == null) {
                continue;
            }
            WriteTask task = resolved.task();
            try {
                task.action().run();
                complete(resolved, true, null);
            } catch (Exception e) {
                logService.error(LogCategory.QUEUE,
                        I18n.get().tr(LangKeys.LOG_STORAGE_QUEUE_TASK_FAILED, task.description()), e);
                complete(resolved, false, e);
            }
        }
    }

    private ResolvedTask resolveTask(QueueItem item) {
        if (item.key() == null) {
            return new ResolvedTask(item.task(), null);
        }
        KeyedTask keyedTask = keyedTasks.remove(item.key());
        if (keyedTask == null) {
            return null;
        }
        return new ResolvedTask(keyedTask.task(), keyedTask.completion());
    }

    private boolean waitForDrain(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (!(queue.isEmpty() && keyedTasks.isEmpty()) && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        if (!queue.isEmpty() || !keyedTasks.isEmpty()) {
            logService.warn(LogCategory.QUEUE,
                    I18n.get().tr(LangKeys.LOG_STORAGE_QUEUE_SHUTDOWN_TIMEOUT, queue.size()));
            return false;
        }
        waitForWorker(Duration.ofMillis(200));
        return true;
    }

    private void waitForWorker(Duration timeout) {
        if (workerFuture == null) {
            return;
        }
        try {
            workerFuture.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception ignored) {
            // Worker will end on its own
        }
    }

    private void cancelWorker() {
        if (workerFuture != null) {
            workerFuture.cancel(true);
        }
    }

    private int drainInline() {
        int executed = 0;
        QueueItem item;
        while ((item = queue.poll()) != null) {
            ResolvedTask resolved = resolveTask(item);
            if (resolved == null) {
                continue;
            }
            WriteTask task = resolved.task();
            try {
                task.action().run();
                complete(resolved, true, null);
                executed++;
            } catch (Exception e) {
                logService.error(LogCategory.QUEUE,
                        I18n.get().tr(LangKeys.LOG_STORAGE_QUEUE_TASK_FAILED, task.description()), e);
                complete(resolved, false, e);
            }
        }
        if (!keyedTasks.isEmpty()) {
            for (KeyedTask keyedTask : keyedTasks.values()) {
                try {
                    keyedTask.task().action().run();
                    complete(new ResolvedTask(keyedTask.task(), keyedTask.completion()), true, null);
                    executed++;
                } catch (Exception e) {
                    logService.error(LogCategory.QUEUE,
                            I18n.get().tr(LangKeys.LOG_STORAGE_QUEUE_TASK_FAILED, keyedTask.task().description()), e);
                    complete(new ResolvedTask(keyedTask.task(), keyedTask.completion()), false, e);
                }
            }
            keyedTasks.clear();
        }
        return executed;
    }

    private OfferResult offerQueueItem(QueueItem item, WriteTask taskForSync, CompletableFuture<Boolean> completion) {
        if (queue.size() < maxQueueSize && queue.offer(item)) {
            return OfferResult.QUEUED;
        }

        logService.warn(LogCategory.QUEUE, I18n.get().tr(LangKeys.LOG_STORAGE_QUEUE_FULL, maxQueueSize));

        return switch (fullPolicy) {
            case DROP -> {
                completeIfPresent(completion, false, null);
                yield OfferResult.DROPPED;
            }
            case SYNC -> runSync(taskForSync, completion);
            case BLOCK -> attemptBlockingOffer(item, taskForSync, completion);
        };
    }

    private OfferResult attemptBlockingOffer(QueueItem item, WriteTask taskForSync, CompletableFuture<Boolean> completion) {
        if (fullTimeoutMs <= 0) {
            return runSync(taskForSync, completion);
        }
        try {
            if (queue.offer(item, fullTimeoutMs, TimeUnit.MILLISECONDS)) {
                return OfferResult.QUEUED;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            completeIfPresent(completion, false, e);
            return OfferResult.DROPPED;
        }
        logService.warn(LogCategory.QUEUE, I18n.get().tr(LangKeys.LOG_STORAGE_QUEUE_FULL_TIMEOUT, fullTimeoutMs));
        return runSync(taskForSync, completion);
    }

    private OfferResult runSync(WriteTask taskForSync, CompletableFuture<Boolean> completion) {
        if (taskForSync == null) {
            completeIfPresent(completion, false, null);
            return OfferResult.DROPPED;
        }
        try {
            taskForSync.action().run();
            completeIfPresent(completion, true, null);
            return OfferResult.EXECUTED;
        } catch (Exception e) {
            completeIfPresent(completion, false, e);
            logService.error(LogCategory.QUEUE,
                    I18n.get().tr(LangKeys.LOG_STORAGE_QUEUE_TASK_FAILED, taskForSync.description()), e);
            return OfferResult.EXECUTED;
        }
    }

    private boolean awaitCompletion(CompletableFuture<Boolean> completion, Duration timeout) {
        try {
            Boolean result = completion.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            return false;
        }
    }

    private void completeIfPresent(CompletableFuture<Boolean> completion, boolean success, Exception error) {
        if (completion == null) {
            return;
        }
        if (success) {
            completion.complete(true);
        } else if (error != null) {
            completion.completeExceptionally(error);
        } else {
            completion.complete(false);
        }
    }

    private void complete(ResolvedTask resolved, boolean success, Exception error) {
        CompletableFuture<Boolean> completion = resolved.completion();
        if (completion == null) {
            return;
        }
        if (success) {
            completion.complete(true);
        } else if (error != null) {
            completion.completeExceptionally(error);
        } else {
            completion.complete(false);
        }
    }

    private String normalizeKey(String key) {
        if (key == null) {
            return null;
        }
        String trimmed = key.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record QueueItem(String key, WriteTask task) {
    }

    private record KeyedTask(WriteTask task, CompletableFuture<Boolean> completion) {
    }

    private record ResolvedTask(WriteTask task, CompletableFuture<Boolean> completion) {
    }

    private enum OfferResult {
        QUEUED,
        EXECUTED,
        DROPPED
    }
}
