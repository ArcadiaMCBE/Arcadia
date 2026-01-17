package Arcadia.ClexaGod.arcadia.storage.repository;

import Arcadia.ClexaGod.arcadia.storage.model.StorageRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.function.Predicate;

public interface StorageRepository<T extends StorageRecord> {

    String getName();

    Optional<T> load(String id);

    void save(T record);

    void delete(String id);

    boolean exists(String id);

    List<T> loadAll();

    default List<T> loadAll(int limit) {
        if (limit <= 0) {
            return List.of();
        }
        List<T> all = loadAll();
        if (all.size() <= limit) {
            return all;
        }
        return new ArrayList<>(all.subList(0, limit));
    }

    long count();

    default List<T> findByFilter(Predicate<T> filter, int limit) {
        Objects.requireNonNull(filter, "filter");
        if (limit <= 0) {
            return List.of();
        }
        List<T> results = new ArrayList<>();
        for (T record : loadAll(limit)) {
            if (filter.test(record)) {
                results.add(record);
                if (results.size() >= limit) {
                    break;
                }
            }
        }
        return results;
    }
}
