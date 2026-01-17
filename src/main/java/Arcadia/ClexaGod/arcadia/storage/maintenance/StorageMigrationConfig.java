package Arcadia.ClexaGod.arcadia.storage.maintenance;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class StorageMigrationConfig {

    private final boolean enabled;
    private final StorageMigrationDirection direction;
    private final boolean dryRun;
    private final int maxRecords;
    private final boolean skipExisting;
}
