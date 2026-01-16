package Arcadia.ClexaGod.arcadia.storage.repository.meta;

import Arcadia.ClexaGod.arcadia.storage.model.StorageRecord;
import lombok.Getter;

@Getter
public final class MetaRecord implements StorageRecord {

    private final String id;
    private final String value;
    private final int dataVersion;

    public MetaRecord(String id, String value) {
        this(id, value, 1);
    }

    public MetaRecord(String id, String value, int dataVersion) {
        this.id = id;
        this.value = value;
        this.dataVersion = dataVersion;
    }
}
