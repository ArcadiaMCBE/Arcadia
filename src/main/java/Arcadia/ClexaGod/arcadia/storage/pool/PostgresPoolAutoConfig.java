package Arcadia.ClexaGod.arcadia.storage.pool;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class PostgresPoolAutoConfig {

    private final boolean enabled;
    private final int minSize;
    private final int maxSize;
    private final int minIdlePercent;
    private final int coresMultiplier;
    private final int reserveCores;
    private final int expectedPlayers;
    private final int playersPerConnection;
}
