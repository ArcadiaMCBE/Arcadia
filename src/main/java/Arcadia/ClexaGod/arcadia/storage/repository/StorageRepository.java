package Arcadia.ClexaGod.arcadia.storage.repository;

import Arcadia.ClexaGod.arcadia.storage.model.StorageRecord;

import java.util.Optional;

public interface StorageRepository<T extends StorageRecord> {

    String getName();

    Optional<T> load(String id);

    void save(T record);

    void delete(String id);

    boolean exists(String id);
}
