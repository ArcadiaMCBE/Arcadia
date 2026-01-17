package Arcadia.ClexaGod.arcadia.storage.retry;

import Arcadia.ClexaGod.arcadia.i18n.LangKeys;
import Arcadia.ClexaGod.arcadia.logging.LogCategory;
import Arcadia.ClexaGod.arcadia.logging.LogService;
import org.allaymc.api.message.I18n;

public final class RetryExecutor {

    private RetryExecutor() {
    }

    public static RetryOutcome run(RetryPolicy policy, LogService logService, LogCategory category, String operation, Runnable action) {
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
                if (logService != null && category != null) {
                    logService.warn(category,
                            I18n.get().tr(LangKeys.LOG_STORAGE_RETRYING, operation, attempt, maxAttempts, delay));
                }
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
