package Arcadia.ClexaGod.arcadia.storage;

import Arcadia.ClexaGod.arcadia.storage.migration.MigrationManager;
import Arcadia.ClexaGod.arcadia.storage.migration.PostgresMigrations;
import Arcadia.ClexaGod.arcadia.logging.LogService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.sql.Connection;

@RequiredArgsConstructor
public final class PostgresStorageProvider implements StorageProvider {

    @Getter
    private final PostgresConfig config;
    private final LogService logService;
    @Getter
    private HikariDataSource dataSource;
    private boolean ready;

    @Override
    public StorageType getType() {
        return StorageType.POSTGRESQL;
    }

    @Override
    public void init() throws Exception {
        Class.forName("org.postgresql.Driver");
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(config.buildJdbcUrl());
        hikari.setDriverClassName("org.postgresql.Driver");
        hikari.setUsername(config.getUsername());
        hikari.setPassword(config.getPassword());
        hikari.setMaximumPoolSize(config.getMaxPoolSize());
        hikari.setMinimumIdle(config.getMinIdle());
        hikari.setConnectionTimeout(config.getConnectionTimeoutMs());
        hikari.setIdleTimeout(config.getIdleTimeoutMs());
        hikari.setPoolName("ArcadiaPool");

        dataSource = new HikariDataSource(hikari);
        try (Connection connection = dataSource.getConnection()) {
            new MigrationManager(dataSource, logService).migrate(PostgresMigrations.list());
            ready = true;
        }
    }

    @Override
    public void close() {
        ready = false;
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Override
    public boolean isReady() {
        return ready;
    }
}
