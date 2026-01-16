package Arcadia.ClexaGod.arcadia.storage.repository.json;

import Arcadia.ClexaGod.arcadia.storage.model.StorageRecord;

public interface JsonCodec<T extends StorageRecord> {

    String encode(T record) throws Exception;

    T decode(String json) throws Exception;
}
