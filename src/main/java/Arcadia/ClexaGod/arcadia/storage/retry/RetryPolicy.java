package Arcadia.ClexaGod.arcadia.storage.retry;

import java.util.concurrent.ThreadLocalRandom;

public record RetryPolicy(boolean enabled, int maxAttempts, long baseDelayMs, long maxDelayMs, long jitterMs) {

    public RetryPolicy {
        if (maxAttempts < 1) {
            maxAttempts = 1;
        }
        if (baseDelayMs < 0) {
            baseDelayMs = 0;
        }
        if (maxDelayMs < baseDelayMs) {
            maxDelayMs = baseDelayMs;
        }
        if (jitterMs < 0) {
            jitterMs = 0;
        }
    }

    public long nextDelayMillis(int attempt) {
        if (attempt <= 1) {
            return addJitter(baseDelayMs);
        }
        long delay = baseDelayMs * (1L << Math.min(20, attempt - 1));
        if (delay < 0) {
            delay = maxDelayMs;
        }
        delay = Math.min(delay, maxDelayMs);
        return addJitter(delay);
    }

    private long addJitter(long delay) {
        if (jitterMs <= 0) {
            return delay;
        }
        long jitter = ThreadLocalRandom.current().nextLong(0, jitterMs + 1);
        return Math.min(maxDelayMs, delay + jitter);
    }

    public static RetryPolicy disabled() {
        return new RetryPolicy(false, 1, 0, 0, 0);
    }
}
