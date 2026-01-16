package Arcadia.ClexaGod.arcadia.storage.migration;

import Arcadia.ClexaGod.arcadia.storage.migration.impl.CreateMetaTableMigration;

import java.util.List;

public final class PostgresMigrations {

    private PostgresMigrations() {
    }

    public static List<Migration> list() {
        return List.of(
                new CreateMetaTableMigration()
        );
    }
}
