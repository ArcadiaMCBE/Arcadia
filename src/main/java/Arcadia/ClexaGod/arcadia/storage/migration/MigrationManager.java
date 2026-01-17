package Arcadia.ClexaGod.arcadia.storage.migration;

import Arcadia.ClexaGod.arcadia.i18n.LangKeys;
import Arcadia.ClexaGod.arcadia.logging.LogCategory;
import Arcadia.ClexaGod.arcadia.logging.LogService;
import lombok.RequiredArgsConstructor;
import org.allaymc.api.message.I18n;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
public final class MigrationManager {

    private static final String VERSION_TABLE = "arcadia_schema_version";

    private final DataSource dataSource;
    private final LogService logService;

    public void migrate(List<Migration> migrations) throws SQLException {
        List<Migration> ordered = migrations.stream()
                .sorted(Comparator.comparingInt(Migration::version))
                .toList();

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            ensureVersionTable(connection);

            int currentVersion = getCurrentVersion(connection);
            int baseVersion = currentVersion;
            List<Migration> pending = ordered.stream()
                    .filter(migration -> migration.version() > baseVersion)
                    .toList();

            if (pending.isEmpty()) {
                logService.info(LogCategory.MIGRATION,
                        I18n.get().tr(LangKeys.LOG_STORAGE_MIGRATION_NONE, currentVersion));
                return;
            }

            for (Migration migration : pending) {
                logService.info(LogCategory.MIGRATION,
                        I18n.get().tr(LangKeys.LOG_STORAGE_MIGRATION_START, migration.version(), migration.description()));
                try {
                    migration.apply(connection);
                    recordVersion(connection, migration.version());
                    connection.commit();
                    currentVersion = migration.version();
                    logService.info(LogCategory.MIGRATION,
                            I18n.get().tr(LangKeys.LOG_STORAGE_MIGRATION_APPLIED, migration.version()));
                } catch (SQLException e) {
                    connection.rollback();
                    logService.error(LogCategory.MIGRATION,
                            I18n.get().tr(LangKeys.LOG_STORAGE_MIGRATION_FAILED, migration.version()), e);
                    throw e;
                }
            }

            logService.info(LogCategory.MIGRATION,
                    I18n.get().tr(LangKeys.LOG_STORAGE_MIGRATION_COMPLETE, currentVersion));
        }
    }

    private void ensureVersionTable(Connection connection) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS arcadia_schema_version (
                    version INTEGER NOT NULL,
                    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """;
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
        if (getCurrentVersion(connection) == 0) {
            recordVersion(connection, 0);
        }
    }

    private int getCurrentVersion(Connection connection) throws SQLException {
        String sql = "SELECT COALESCE(MAX(version), 0) FROM " + VERSION_TABLE;
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        }
        return 0;
    }

    private void recordVersion(Connection connection, int version) throws SQLException {
        String sql = "INSERT INTO " + VERSION_TABLE + " (version) VALUES (?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, version);
            ps.executeUpdate();
        }
    }
}
