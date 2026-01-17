package Arcadia.ClexaGod.arcadia.storage.maintenance;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class StorageSeedConfig {

    private final boolean enabled;
    private final boolean seedOwner;
    private final boolean seedServerName;
    private final boolean seedCreatedAt;
}
