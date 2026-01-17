package Arcadia.ClexaGod.arcadia;

import Arcadia.ClexaGod.arcadia.config.ConfigService;
import Arcadia.ClexaGod.arcadia.i18n.LangKeys;
import Arcadia.ClexaGod.arcadia.i18n.MessageService;
import Arcadia.ClexaGod.arcadia.logging.LogCategory;
import Arcadia.ClexaGod.arcadia.logging.LogService;
import Arcadia.ClexaGod.arcadia.module.ModuleRegistry;
import Arcadia.ClexaGod.arcadia.module.impl.SystemModule;
import Arcadia.ClexaGod.arcadia.storage.StorageManager;
import lombok.Getter;
import org.allaymc.api.message.I18n;
import org.allaymc.api.plugin.Plugin;

public class ArcadiaCore extends Plugin {

    private static ArcadiaCore instance;

    @Getter
    private ConfigService configService;
    @Getter
    private ModuleRegistry moduleRegistry;
    @Getter
    private MessageService messageService;
    @Getter
    private StorageManager storageManager;
    @Getter
    private LogService logService;

    public static ArcadiaCore getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;
        messageService = new MessageService();
        configService = new ConfigService(getPluginContainer().dataFolder(), getClass().getClassLoader(), getPluginLogger());
        configService.load();
        logService = new LogService(getPluginLogger(), configService.getCoreConfig().getLogConfig(), configService.getCoreConfig().isDebug());
        configService.setLogService(logService);

        storageManager = new StorageManager(logService, getPluginContainer().dataFolder(), this);
        storageManager.init(configService.getCoreConfig());

        moduleRegistry = new ModuleRegistry(logService, configService);
        moduleRegistry.register(new SystemModule());
    }

    @Override
    public void onEnable() {
        logService.info(LogCategory.CORE, I18n.get().tr(LangKeys.LOG_CORE_ENABLING));
        moduleRegistry.enableAll(this);
        if (storageManager != null) {
            storageManager.scheduleWarmUp();
        }
        logService.info(LogCategory.CORE, I18n.get().tr(LangKeys.LOG_CORE_ENABLED));
    }

    @Override
    public void onDisable() {
        logService.info(LogCategory.CORE, I18n.get().tr(LangKeys.LOG_CORE_DISABLING));
        if (moduleRegistry != null) {
            moduleRegistry.disableAll();
        }
        if (storageManager != null) {
            storageManager.close();
        }
        logService.info(LogCategory.CORE, I18n.get().tr(LangKeys.LOG_CORE_DISABLED));
    }

    @Override
    public boolean isReloadable() {
        return true;
    }

    @Override
    public void reload() {
        logService.info(LogCategory.CORE, I18n.get().tr(LangKeys.LOG_CORE_RELOADING));
        try {
            if (moduleRegistry != null) {
                moduleRegistry.disableAll();
            }
            if (storageManager != null) {
                storageManager.close();
            }
            if (configService != null) {
                configService.reload();
            }
            if (logService != null && configService != null) {
                logService.update(configService.getCoreConfig().getLogConfig(), configService.getCoreConfig().isDebug());
            }
            if (storageManager != null && configService != null) {
                storageManager.init(configService.getCoreConfig());
            }
            if (moduleRegistry != null) {
                moduleRegistry.enableAll(this);
            }
            if (storageManager != null) {
                storageManager.scheduleWarmUp();
            }
            logService.info(LogCategory.CORE, I18n.get().tr(LangKeys.LOG_CORE_RELOADED));
        } catch (Exception e) {
            logService.error(LogCategory.CORE, I18n.get().tr(LangKeys.LOG_CORE_RELOAD_FAILED), e);
        }
    }
}
