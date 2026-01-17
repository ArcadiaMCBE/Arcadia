package Arcadia.ClexaGod.arcadia.module.impl;

import Arcadia.ClexaGod.arcadia.ArcadiaCore;
import Arcadia.ClexaGod.arcadia.i18n.LangKeys;
import Arcadia.ClexaGod.arcadia.module.Module;
import Arcadia.ClexaGod.arcadia.storage.cache.CacheEventListener;
import Arcadia.ClexaGod.arcadia.storage.repository.StorageRepository;
import Arcadia.ClexaGod.arcadia.storage.repository.meta.MetaRecord;
import Arcadia.ClexaGod.arcadia.storage.repository.meta.MetaRepositoryFactory;
import org.allaymc.api.server.Server;
import org.allaymc.api.message.I18n;

public final class SystemModule implements Module {

    private StorageRepository<MetaRecord> metaRepository;
    private CacheEventListener cacheEventListener;

    @Override
    public String getName() {
        return "system";
    }

    @Override
    public void onEnable(ArcadiaCore core) {
        metaRepository = MetaRepositoryFactory
                .create(core.getStorageManager(), core.getPluginLogger())
                .orElse(null);
        cacheEventListener = new CacheEventListener(core.getStorageManager());
        Server.getInstance().getEventBus().registerListener(cacheEventListener);
        core.getPluginLogger().info(I18n.get().tr(LangKeys.LOG_SYSTEM_READY, core.getConfigService().getCoreConfig().getOwner()));
    }

    @Override
    public void onDisable() {
        if (cacheEventListener != null) {
            Server.getInstance().getEventBus().unregisterListener(cacheEventListener);
            cacheEventListener = null;
        }
    }
}
