package Arcadia.ClexaGod.arcadia.storage.maintenance;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class StorageHealthConfig {

    private final boolean enabled;
    private final boolean logOnStartup;
    private final boolean jsonWriteTest;
    private final boolean postgresTest;
    private final int connectionTimeoutMs;
}
