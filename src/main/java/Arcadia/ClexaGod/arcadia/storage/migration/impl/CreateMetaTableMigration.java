package Arcadia.ClexaGod.arcadia.storage.migration.impl;

import Arcadia.ClexaGod.arcadia.storage.migration.Migration;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class CreateMetaTableMigration implements Migration {

    @Override
    public int version() {
        return 1;
    }

    @Override
    public String description() {
        return "Create arcadia_meta table";
    }

    @Override
    public void apply(Connection connection) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS arcadia_meta (
                    key VARCHAR(64) PRIMARY KEY,
                    value TEXT NOT NULL,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """;
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
