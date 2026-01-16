package Arcadia.ClexaGod.arcadia.storage;

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

    public PostgresConfig(String host, int port, String database, String username, String password, boolean ssl,
                          int maxPoolSize, int minIdle, int connectionTimeoutMs, int idleTimeoutMs) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.ssl = ssl;
        this.maxPoolSize = maxPoolSize;
        this.minIdle = minIdle;
        this.connectionTimeoutMs = connectionTimeoutMs;
        this.idleTimeoutMs = idleTimeoutMs;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDatabase() {
        return database;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isSsl() {
        return ssl;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public int getMinIdle() {
        return minIdle;
    }

    public int getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public int getIdleTimeoutMs() {
        return idleTimeoutMs;
    }

    public boolean isValid() {
        return host != null && !host.isBlank()
                && database != null && !database.isBlank()
                && username != null && !username.isBlank()
                && port > 0 && port <= 65535;
    }
}
