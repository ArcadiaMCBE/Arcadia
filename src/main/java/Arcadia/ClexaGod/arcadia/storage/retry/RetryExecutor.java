package Arcadia.ClexaGod.arcadia.storage.retry;

import Arcadia.ClexaGod.arcadia.i18n.LangKeys;
import org.allaymc.api.message.I18n;
import org.slf4j.Logger;

public final class RetryExecutor {

    private RetryExecutor() {
    }

    public static RetryOutcome run(RetryPolicy policy, Logger logger, String operation, Runnable action) {
        RetryPolicy effective = policy != null ? policy : RetryPolicy.disabled();
        int maxAttempts = Math.max(1, effective.maxAttempts());
        int attempt = 0;
        Exception last = null;
        while (attempt < maxAttempts) {
            attempt++;
            try {
                action.run();
                return new RetryOutcome(true, null, attempt);
            } catch (Exception e) {
                last = e;
                if (!effective.enabled() || attempt >= maxAttempts) {
                    break;
                }
                long delay = effective.nextDelayMillis(attempt);
                logger.warn(I18n.get().tr(LangKeys.LOG_STORAGE_RETRYING, operation, attempt, maxAttempts, delay));
                if (delay > 0) {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        return new RetryOutcome(false, last, attempt);
    }
}
