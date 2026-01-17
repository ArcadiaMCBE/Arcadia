package Arcadia.ClexaGod.arcadia.storage.pool;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class PostgresPoolSizing {

    private final boolean enabled;
    private final int availableCores;
    private final int reservedCores;
    private final int usableCores;
    private final int targetFromCores;
    private final int targetFromPlayers;
    private final int maxPoolSize;
    private final int minIdle;
}
