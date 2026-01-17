package Arcadia.ClexaGod.arcadia.storage.retry;

public record RetryOutcome(boolean success, Exception error, int attempts) {
}
