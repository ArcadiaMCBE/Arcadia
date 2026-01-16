package Arcadia.ClexaGod.arcadia;

import Arcadia.ClexaGod.arcadia.config.ConfigService;
import Arcadia.ClexaGod.arcadia.i18n.LangKeys;
import Arcadia.ClexaGod.arcadia.i18n.MessageService;
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

    public static ArcadiaCore getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;
        messageService = new MessageService();
        configService = new ConfigService(getPluginContainer().dataFolder(), getClass().getClassLoader(), getPluginLogger());
        configService.load();

        storageManager = new StorageManager(getPluginLogger(), getPluginContainer().dataFolder(), this);
        storageManager.init(configService.getCoreConfig());

        moduleRegistry = new ModuleRegistry(getPluginLogger(), configService);
        moduleRegistry.register(new SystemModule());
    }

    @Override
    public void onEnable() {
        getPluginLogger().info(I18n.get().tr(LangKeys.LOG_CORE_ENABLING));
        moduleRegistry.enableAll(this);
        getPluginLogger().info(I18n.get().tr(LangKeys.LOG_CORE_ENABLED));
    }

    @Override
    public void onDisable() {
        getPluginLogger().info(I18n.get().tr(LangKeys.LOG_CORE_DISABLING));
        if (moduleRegistry != null) {
            moduleRegistry.disableAll();
        }
        if (storageManager != null) {
            storageManager.close();
        }
        getPluginLogger().info(I18n.get().tr(LangKeys.LOG_CORE_DISABLED));
    }
}
