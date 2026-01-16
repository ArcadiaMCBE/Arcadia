package Arcadia.ClexaGod.arcadia.storage;

import Arcadia.ClexaGod.arcadia.storage.migration.MigrationManager;
import Arcadia.ClexaGod.arcadia.storage.migration.PostgresMigrations;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;

import java.sql.Connection;

@RequiredArgsConstructor
public final class PostgresStorageProvider implements StorageProvider {

    private final PostgresConfig config;
    private final Logger logger;
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
        hikari.setJdbcUrl(buildJdbcUrl());
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
            new MigrationManager(dataSource, logger).migrate(PostgresMigrations.list());
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

    private String buildJdbcUrl() {
        StringBuilder url = new StringBuilder();
        url.append("jdbc:postgresql://")
                .append(config.getHost())
                .append(":")
                .append(config.getPort())
                .append("/")
                .append(config.getDatabase());
        if (config.isSsl()) {
            url.append("?ssl=true");
        }
        return url.toString();
    }
}
