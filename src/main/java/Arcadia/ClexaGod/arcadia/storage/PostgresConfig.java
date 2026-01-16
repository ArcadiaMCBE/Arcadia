package Arcadia.ClexaGod.arcadia.storage;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class PostgresConfig {

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final boolean ssl;
    private final int maxPoolSize;
    private final int minIdle;
    private final int connectionTimeoutMs;
    private final int idleTimeoutMs;

    public boolean isValid() {
        return host != null && !host.isBlank()
                && database != null && !database.isBlank()
                && username != null && !username.isBlank()
                && port > 0 && port <= 65535;
    }
}
