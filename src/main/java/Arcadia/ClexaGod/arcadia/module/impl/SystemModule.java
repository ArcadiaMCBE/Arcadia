package Arcadia.ClexaGod.arcadia.module.impl;

import Arcadia.ClexaGod.arcadia.ArcadiaCore;
import Arcadia.ClexaGod.arcadia.command.ArcadiaCommand;
import Arcadia.ClexaGod.arcadia.i18n.LangKeys;
import Arcadia.ClexaGod.arcadia.logging.LogCategory;
import Arcadia.ClexaGod.arcadia.module.Module;
import Arcadia.ClexaGod.arcadia.storage.cache.CacheEventListener;
import Arcadia.ClexaGod.arcadia.storage.maintenance.StorageMaintenanceService;
import Arcadia.ClexaGod.arcadia.storage.repository.StorageRepository;
import Arcadia.ClexaGod.arcadia.storage.repository.meta.MetaRecord;
import Arcadia.ClexaGod.arcadia.storage.repository.meta.MetaRepositoryFactory;
import org.allaymc.api.server.Server;
import org.allaymc.api.message.I18n;
import org.allaymc.api.registry.Registries;

public final class SystemModule implements Module {

    private StorageRepository<MetaRecord> metaRepository;
    private CacheEventListener cacheEventListener;
    private StorageMaintenanceService maintenanceService;
    private ArcadiaCommand arcadiaCommand;

    @Override
    public String getName() {
        return "system";
    }

    @Override
    public void onEnable(ArcadiaCore core) {
        metaRepository = MetaRepositoryFactory
                .create(core.getStorageManager(), core.getLogService())
                .orElse(null);
        arcadiaCommand = new ArcadiaCommand(core);
        Registries.COMMANDS.register(arcadiaCommand);
        cacheEventListener = new CacheEventListener(core.getStorageManager());
        Server.getInstance().getEventBus().registerListener(cacheEventListener);
        maintenanceService = new StorageMaintenanceService(
                core.getConfigService().getCoreConfig(),
                core.getStorageManager(),
                metaRepository,
                core.getPluginContainer().dataFolder(),
                core.getLogService()
        );
        maintenanceService.runAsync(core);
        core.getLogService().info(LogCategory.MODULE,
                I18n.get().tr(LangKeys.LOG_SYSTEM_READY, core.getConfigService().getCoreConfig().getOwner()));
    }

    @Override
    public void onDisable() {
        if (cacheEventListener != null) {
            Server.getInstance().getEventBus().unregisterListener(cacheEventListener);
            cacheEventListener = null;
        }
        if (arcadiaCommand != null) {
            Registries.COMMANDS.unregister(arcadiaCommand.getName());
            arcadiaCommand = null;
        }
        maintenanceService = null;
    }
}
