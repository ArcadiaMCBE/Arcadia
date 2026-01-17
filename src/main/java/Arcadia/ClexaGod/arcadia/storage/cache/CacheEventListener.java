package Arcadia.ClexaGod.arcadia.storage.cache;

import Arcadia.ClexaGod.arcadia.storage.StorageManager;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.api.eventbus.event.server.PlayerDisconnectEvent;
import org.allaymc.api.eventbus.event.server.PlayerQuitEvent;

public final class CacheEventListener {

    private final StorageManager storageManager;

    public CacheEventListener(StorageManager storageManager) {
        this.storageManager = storageManager;
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent event) {
        flushIfEnabled();
    }

    @EventHandler
    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        flushIfEnabled();
    }

    private void flushIfEnabled() {
        StorageCacheManager cacheManager = storageManager.getCacheManager();
        if (cacheManager == null || !cacheManager.isFlushOnPlayerQuit()) {
            return;
        }
        storageManager.flushCachesAsync();
    }
}
