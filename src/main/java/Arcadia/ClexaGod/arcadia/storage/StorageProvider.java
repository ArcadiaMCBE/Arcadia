package Arcadia.ClexaGod.arcadia.storage;

public interface StorageProvider {

    StorageType getType();

    void init() throws Exception;

    void close();

    boolean isReady();
}
