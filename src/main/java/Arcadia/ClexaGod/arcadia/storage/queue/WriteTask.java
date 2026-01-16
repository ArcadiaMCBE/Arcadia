package Arcadia.ClexaGod.arcadia.storage.queue;

public record WriteTask(String key, String description, Runnable action) {
}
