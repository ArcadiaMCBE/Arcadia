package Arcadia.ClexaGod.arcadia.storage.queue;

import Arcadia.ClexaGod.arcadia.i18n.LangKeys;
import lombok.Getter;
import org.allaymc.api.message.I18n;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AsyncWriteQueue {

    private final ExecutorService executor;
    private final Logger logger;
    private final int maxQueueSize;
    private final BlockingQueue<QueueItem> queue = new LinkedBlockingQueue<>();
    private final ConcurrentMap<String, WriteTask> keyedTasks = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Future<?> workerFuture;

    @Getter
    private volatile boolean started;

    public AsyncWriteQueue(ExecutorService executor, Logger logger, int maxQueueSize) {
        this.executor = Objects.requireNonNull(executor, "executor");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.maxQueueSize = Math.max(1, maxQueueSize);
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        workerFuture = executor.submit(this::runLoop);
        started = true;
        logger.info(I18n.get().tr(LangKeys.LOG_STORAGE_QUEUE_STARTED));
    }

    public boolean enqueue(WriteTask task) {
        if (task == null) {
            return false;
        }
        if (queue.size() >= maxQueueSize) {
            logger.warn(I18n.get().tr(LangKeys.LOG_STORAGE_QUEUE_FULL, maxQueueSize));
            return false;
        }

        String key = normalizeKey(task.key());
        if (key == null) {
            return queue.offer(new QueueItem(null, task));
        }

        WriteTask previous = keyedTasks.put(key, task);
        if (previous == null) {
            return queue.offer(new QueueItem(key, null));
        }
        return true;
    }

    public void shutdown(Duration timeout) {
        running.set(false);
        logger.info(I18n.get().tr(LangKeys.LOG_STORAGE_QUEUE_STOPPING));
        waitForDrain(timeout);
        started = false;
        logger.info(I18n.get().tr(LangKeys.LOG_STORAGE_QUEUE_STOPPED));
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
            WriteTask task = resolveTask(item);
            if (task == null) {
                continue;
            }
            try {
                task.action().run();
            } catch (Exception e) {
                logger.error(I18n.get().tr(LangKeys.LOG_STORAGE_QUEUE_TASK_FAILED, task.description()), e);
            }
        }
    }

    private WriteTask resolveTask(QueueItem item) {
        if (item.key() == null) {
            return item.task();
        }
        return keyedTasks.remove(item.key());
    }

    private void waitForDrain(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (!queue.isEmpty() && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        if (!queue.isEmpty()) {
            logger.warn(I18n.get().tr(LangKeys.LOG_STORAGE_QUEUE_SHUTDOWN_TIMEOUT, queue.size()));
        }
        if (workerFuture != null) {
            try {
                workerFuture.get(100, TimeUnit.MILLISECONDS);
            } catch (Exception ignored) {
                // Worker will end on its own
            }
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
}
